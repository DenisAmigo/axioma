package ru.amigo.axioma.insurance.exception;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 23:57
 */
public class PolicyAccessDeniedException extends RuntimeException {
    public PolicyAccessDeniedException(Long accountId) {
        super("Доступ к счёту запрещён: " + accountId);
    }
}
