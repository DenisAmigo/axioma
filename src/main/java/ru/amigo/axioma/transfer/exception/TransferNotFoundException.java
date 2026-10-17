package ru.amigo.axioma.transfer.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:00
 */
public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(Long id) {
        super("Перевод не найден с идентификатором: " + id);
    }
}
