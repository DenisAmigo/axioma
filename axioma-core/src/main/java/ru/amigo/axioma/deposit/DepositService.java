package ru.amigo.axioma.deposit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.AccountStatus;
import ru.amigo.axioma.account.exception.AccountNotFoundException;
import ru.amigo.axioma.deposit.dto.DepositResponse;
import ru.amigo.axioma.ledger.EntryType;
import ru.amigo.axioma.ledger.LedgerEntry;
import ru.amigo.axioma.ledger.LedgerEntryRepository;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 21:51
 */
@Service
@Transactional(readOnly = true)
public class DepositService {

    private final AccountRepository accountRepository;
    private final DepositTransactionRepository depositRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public DepositService(
            AccountRepository accountRepository,
            DepositTransactionRepository depositRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.accountRepository = accountRepository;
        this.depositRepository = depositRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public DepositResponse processAtmDeposit(Long accountId, BigDecimal amount, String atmId, String traceNumber) {
        // 1. Проверка идемпотентности
        var existing = depositRepository.findByIdempotencyKey(traceNumber);
        if (existing.isPresent()) {
            return DepositResponse.from(existing.get());
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Счёт неактивен");
        }

        // 2. Создаем транзакцию в статусе CONFIRMED
        DepositTransaction depositTx = new DepositTransaction(account, amount, account.getCurrency());
        depositTx.setStatus(DepositStatus.CONFIRMED);
        depositTx.setAtmTransactionId(traceNumber);
        depositTx.setIdempotencyKey(traceNumber);
        depositTx.setCompletedAt(Instant.now());

        // 3. Зачисляем деньги
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Пишем в леджер
        // ВНИМАНИЕ: transfer = null, так как это депозит
        LedgerEntry entry = new LedgerEntry(null, account, EntryType.CREDIT, amount, account.getBalance());
        ledgerEntryRepository.save(entry);

        return DepositResponse.from(depositRepository.save(depositTx));
    }
}
