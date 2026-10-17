package ru.amigo.axioma.transfer.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:01
 */
public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException(Long accountId) {
        super("Нельзя перевести средства на тот же счёт: " + accountId);
    }
}
