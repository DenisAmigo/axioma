package ru.amigo.axioma.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 20:16
 */
public record CreatePolicyRequest(
        @NotNull(message = "Не заполнен accountId")
        Long accountId,

        @NotBlank(message = "Не заполнен код компании")
        String companyCode,

        @NotNull(message = "Требуется заполнить сумму")
        @Positive(message = "Сумма должна быть положительной")
        BigDecimal amount
) {}
