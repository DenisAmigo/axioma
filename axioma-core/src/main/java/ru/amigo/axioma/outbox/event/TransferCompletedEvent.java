package ru.amigo.axioma.outbox.event;

import ru.amigo.axioma.account.Currency;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 1:06
 */
public record TransferCompletedEvent(
        Long transferId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        Currency currency
) {}
