package ru.amigo.axioma.user.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 2:26
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String field, String value) {
        super("Пользователь уже существует с " + field + ": " + value);
    }
}
