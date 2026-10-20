package ru.amigo.axioma.transfer.exception;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:00
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId, BigDecimal required, BigDecimal available) {
        super("Недостаточно средств на счёте " + accountId +
                ". Требуется: " + required + ", доступно: " + available);
    }
}
