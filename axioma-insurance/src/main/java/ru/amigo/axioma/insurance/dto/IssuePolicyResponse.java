package ru.amigo.axioma.insurance.dto;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 20:36
 */
public record IssuePolicyResponse(
        boolean success,
        String policyNumber,
        String message
) {}
