package ru.amigo.axioma.insurance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.exception.AccountNotFoundException;
import ru.amigo.axioma.insurance.dto.CreatePolicyRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteResponse;
import ru.amigo.axioma.insurance.dto.IssuePolicyResponse;
import ru.amigo.axioma.insurance.dto.PolicyResponse;
import ru.amigo.axioma.insurance.exception.InsufficientAvailableFundsException;
import ru.amigo.axioma.ledger.EntryType;
import ru.amigo.axioma.ledger.LedgerEntry;
import ru.amigo.axioma.ledger.LedgerEntryRepository;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;
import ru.amigo.axioma.user.exception.UserNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 19:02
 */
@Service
public class InsuranceService {

    private final InsuranceQuoteClient quoteClient;
    private final AccountRepository accountRepository;
    private final InsurancePolicyRepository policyRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final InsuranceQuoteRepository quoteRepository;
    private final UserRepository userRepository;

    public InsuranceService(
            InsuranceQuoteClient quoteClient,
            AccountRepository accountRepository,
            InsurancePolicyRepository policyRepository,
            LedgerEntryRepository ledgerEntryRepository,
            InsuranceQuoteRepository quoteRepository,
            UserRepository userRepository
    ) {
        this.quoteClient = quoteClient;
        this.accountRepository = accountRepository;
        this.policyRepository = policyRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.quoteRepository = quoteRepository;
        this.userRepository = userRepository;
    }

    public List<InsuranceQuoteResponse> getQuotes(Long userId, InsuranceQuoteRequest request) {
        try {
            // Ждем результат максимум 3 секунды
            CompletableFuture<List<InsuranceQuoteResponse>> future = quoteClient.getQuotesAsync(request);

            // Если страховая не ответит за 3 секунды, мы отдадим пустой список (таймаут)
            List<InsuranceQuoteResponse> responses = future.orTimeout(3, java.util.concurrent.TimeUnit.SECONDS).join();

            if (responses != null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

                // Сохраняем каждое предложение
                for (InsuranceQuoteResponse quote : responses) {
                    InsuranceQuote entity = new InsuranceQuote(
                            user,
                            quote.companyCode(),
                            quote.amount(),
                            "region=" + request.region() + ", age=" + request.age() + ", hp=" + request.horsePower()
                    );
                    quoteRepository.save(entity);
                }

                // Сортируем по цене (от дешевой к дорогой)
                return responses.stream()
                        .sorted((a, b) -> a.amount().compareTo(b.amount()))
                        .toList();
            }
            return List.of();

        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить расчет ОСАГО", e);
        }
    }

    @Transactional
    public PolicyResponse createPolicy(Long userId, CreatePolicyRequest request) {
        // 1. Найти счет
        Account account = accountRepository.findByIdForUpdate(request.accountId())
                .orElseThrow(() -> new AccountNotFoundException(request.accountId()));

        // 2. Проверить, что это счет данного пользователя
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Чужой счет");
        }

        // 3. Проверить доступный баланс (balance - heldAmount >= amount)
        if (account.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientAvailableFundsException(account.getAvailableBalance(), request.amount());
        }

        // 4. Сделать холд
        account.setHeldAmount(account.getHeldAmount().add(request.amount()));
        accountRepository.save(account);

        // 5. Создать полис в статусе PENDING с expiresAt = now + 20 минут
        User user = account.getUser();
        Instant expiresAt = Instant.now().plusSeconds(20 * 60);
        InsurancePolicy policy = new InsurancePolicy(user, account, request.companyCode(), request.amount(), expiresAt);

        InsurancePolicy savedPolicy = policyRepository.save(policy);
        return PolicyResponse.from(savedPolicy);
    }

    @Transactional
    public PolicyResponse confirmPolicy(Long userId, Long policyId) {
        // 1. Блокируем полис и счет
        InsurancePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Полис не найден"));

        if (!policy.getUser().getId().equals(userId)) {
            throw new RuntimeException("Чужой полис");
        }
        if (policy.getStatus() != PolicyStatus.PENDING) {
            throw new RuntimeException("Полис не в статусе PENDING");
        }

        Account account = accountRepository.findByIdForUpdate(policy.getAccount().getId())
                .orElseThrow(() -> new RuntimeException("Счет не найден"));

        // 2. Идем в Страховую
        IssuePolicyResponse response = quoteClient.issuePolicy(policy.getCompanyCode(), policy.getAmount());

        if (response.success()) {
            // 3. Успех: списываем деньги и снимаем холд
            policy.setStatus(PolicyStatus.ISSUED);
            policy.setPolicyNumber(response.policyNumber());
            policy.setCompletedAt(Instant.now());

            account.setBalance(account.getBalance().subtract(policy.getAmount()));
            account.setHeldAmount(account.getHeldAmount().subtract(policy.getAmount()));
            accountRepository.save(account);

            // Пишем в леджер (DEBIT)
            LedgerEntry entry = new LedgerEntry(null, account, EntryType.DEBIT, policy.getAmount(), account.getBalance());
            ledgerEntryRepository.save(entry);

        } else {
            // 4. Компенсация: страховая отказала, снимаем холд
            policy.setStatus(PolicyStatus.CANCELLED);
            policy.setCompletedAt(Instant.now());
            account.setHeldAmount(account.getHeldAmount().subtract(policy.getAmount()));
            accountRepository.save(account);
        }

        return PolicyResponse.from(policyRepository.save(policy));
    }
}
