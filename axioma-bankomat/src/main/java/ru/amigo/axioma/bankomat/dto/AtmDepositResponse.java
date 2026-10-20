package ru.amigo.axioma.bankomat.dto;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 20:43
 */
public record AtmDepositResponse(
        boolean success,
        String transactionId,
        String message
) {}
