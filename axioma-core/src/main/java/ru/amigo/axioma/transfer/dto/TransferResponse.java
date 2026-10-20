package ru.amigo.axioma.transfer.dto;

import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.transfer.Transfer;
import ru.amigo.axioma.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 16:59
 */
public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        String description,
        String failureReason,
        Instant createdAt,
        Instant completedAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}
