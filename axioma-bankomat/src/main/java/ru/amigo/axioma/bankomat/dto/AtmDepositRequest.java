package ru.amigo.axioma.bankomat.dto;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 20:43
 */
public record AtmDepositRequest(
        String accountNumber,
        BigDecimal amount
) {}
