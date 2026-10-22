package ru.amigo.axioma.insurance.exception;

import java.math.BigDecimal;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 22:55
 */
public class InsufficientAvailableFundsException extends RuntimeException {
    public InsufficientAvailableFundsException(BigDecimal available, BigDecimal required) {
        super("Недостаточно доступных средств. Доступно: " + available + ", требуется: " + required);
    }
}
