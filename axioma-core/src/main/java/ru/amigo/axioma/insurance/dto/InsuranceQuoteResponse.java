package ru.amigo.axioma.insurance.dto;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:55
 */
public record InsuranceQuoteResponse(
        String companyCode,
        String companyName,
        BigDecimal amount
) {}
