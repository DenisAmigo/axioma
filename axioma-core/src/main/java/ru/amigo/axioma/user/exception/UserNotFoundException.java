package ru.amigo.axioma.user.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 2:25
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Пользователь не найден с идентификатором: " + id);
    }

    public UserNotFoundException(String username) {
        super("Пользователь не найден с именем пользователя: " + username);
    }
}
