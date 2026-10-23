package ru.amigo.axioma.outbox.event;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 1:13
 */
public record PolicyIssuedEvent(
        Long policyId,
        String policyNumber,
        String email,
        BigDecimal amount
) {}
