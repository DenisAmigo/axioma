package ru.amigo.axioma.transfer.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:01
 */
public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(Long accountId, String status) {
        super("Счёт " + accountId + " неактивен. Статус: " + status);
    }
}
