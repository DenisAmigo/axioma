package ru.amigo.axioma.account.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:43
 */
public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException(Long accountId) {
        super("Доступ к счёту запрещён: " + accountId);
    }
}
