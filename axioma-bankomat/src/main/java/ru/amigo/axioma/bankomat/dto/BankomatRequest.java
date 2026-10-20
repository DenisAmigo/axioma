package ru.amigo.axioma.bankomat.dto;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 23:29
 */
public record BankomatRequest(
        Long accountId,
        BigDecimal amount,
        boolean simulateFailure
) {}
