package ru.amigo.axioma.deposit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 20:43
 */
public record AtmDepositRequest(
        @NotNull Long accountId,
        @NotNull @Positive BigDecimal amount,
        @NotNull String atmId,
        @NotNull String traceNumber
) {}
