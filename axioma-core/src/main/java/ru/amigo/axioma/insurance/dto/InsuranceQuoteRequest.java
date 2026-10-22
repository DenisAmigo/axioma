package ru.amigo.axioma.insurance.dto;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:54
 */
public record InsuranceQuoteRequest(
        String region,
        int age,
        int experienceYears,
        int horsePower,
        boolean isBlacklisted
) {}
