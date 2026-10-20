package ru.amigo.axioma.deposit.dto;

import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.deposit.DepositStatus;
import ru.amigo.axioma.deposit.DepositTransaction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 22:36
 */
public record DepositResponse(
        Long id,
        Long accountId,
        BigDecimal amount,
        Currency currency,
        DepositStatus status,
        String atmTransactionId,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
    public static DepositResponse from(DepositTransaction tx) {
        return new DepositResponse(
                tx.getId(),
                tx.getAccount().getId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getAtmTransactionId(),
                tx.getFailureReason(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
