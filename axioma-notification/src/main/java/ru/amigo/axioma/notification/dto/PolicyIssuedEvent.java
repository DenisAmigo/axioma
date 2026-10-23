package ru.amigo.axioma.notification.dto;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 16:24
 */
public record PolicyIssuedEvent(
        Long policyId,
        String policyNumber,
        String email,
        BigDecimal amount
) {}
