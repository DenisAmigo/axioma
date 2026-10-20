package ru.amigo.axioma.account.dto;

import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountStatus;
import ru.amigo.axioma.account.Currency;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:41
 */
public record AccountResponse(
        Long id,
        String accountNumber,
        Long userId,
        Currency currency,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getUser().getId(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
