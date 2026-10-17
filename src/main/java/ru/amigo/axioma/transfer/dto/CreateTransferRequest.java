package ru.amigo.axioma.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 16:55
 */
public record CreateTransferRequest(
        @NotNull(message = "Не заполнен fromAccountId")
        Long fromAccountId,

        @NotNull(message = "Не заполнен toAccountId")
        Long toAccountId,

        @NotNull(message = "Требуется заполнить сумму")
        @Positive(message = "Сумма должна быть положительной")
        BigDecimal amount,

        String description
) {}
