package ru.amigo.axioma.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.account.dto.AccountResponse;
import ru.amigo.axioma.account.dto.OpenAccountRequest;
import ru.amigo.axioma.account.exception.AccountAccessDeniedException;
import ru.amigo.axioma.account.exception.AccountHasBalanceException;
import ru.amigo.axioma.account.exception.AccountNotFoundException;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;
import ru.amigo.axioma.user.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.security.SecureRandom;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:50
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse open(OpenAccountRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        String accountNumber = generateUniqueAccountNumber();
        Account account = new Account(user, accountNumber, request.currency());
        Account saved = accountRepository.save(account);
        return AccountResponse.from(saved);
    }

    public AccountResponse getById(Long accountId, Long userId) {
        Account account = getOwnedAccount(accountId, userId);
        return AccountResponse.from(account);
    }

    public Page<AccountResponse> listByUser(Long userId, Pageable pageable) {
        return accountRepository.findByUserId(userId, pageable)
                .map(AccountResponse::from);
    }

    @Transactional
    public void close(Long accountId, Long userId) {
        Account account = getOwnedAccount(accountId, userId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            return; // уже закрыт — идемпотентно
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new AccountHasBalanceException(account.getId());
        }
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    private Account getOwnedAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!account.getUser().getId().equals(userId)) {
            throw new AccountAccessDeniedException(accountId);
        }
        return account;
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String number = generateAccountNumber();
            if (!accountRepository.existsByAccountNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Не удалось сгенерировать уникальный номер счёта");
    }

    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder("40817");
        for (int i = 0; i < 15; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
