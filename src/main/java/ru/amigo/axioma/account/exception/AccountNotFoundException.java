package ru.amigo.axioma.account.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:42
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Счёт не найден с идентификатором: " + id);
    }
}
