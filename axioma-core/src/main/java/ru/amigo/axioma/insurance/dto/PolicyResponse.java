package ru.amigo.axioma.insurance.dto;

import ru.amigo.axioma.insurance.InsurancePolicy;
import ru.amigo.axioma.insurance.PolicyStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 20:19
 */
public record PolicyResponse(
        Long id,
        Long accountId,
        String companyCode,
        BigDecimal amount,
        PolicyStatus status,
        String policyNumber,
        Instant createdAt,
        Instant expiresAt,
        Instant completedAt
) {
    public static PolicyResponse from(InsurancePolicy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getAccount().getId(),
                policy.getCompanyCode(),
                policy.getAmount(),
                policy.getStatus(),
                policy.getPolicyNumber(),
                policy.getCreatedAt(),
                policy.getExpiresAt(),
                policy.getCompletedAt()
        );
    }
}
