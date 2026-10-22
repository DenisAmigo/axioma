package ru.amigo.axioma.insurance.dto;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 20:35
 */
public record IssuePolicyRequest(
        String companyCode,
        BigDecimal amount
) {}
