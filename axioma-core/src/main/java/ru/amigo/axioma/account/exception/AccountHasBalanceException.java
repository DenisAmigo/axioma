package ru.amigo.axioma.account.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:43
 */
public class AccountHasBalanceException extends RuntimeException {

    public AccountHasBalanceException(Long accountId) {
        super("Невозможно закрыть счёт с ненулевым балансом: " + accountId);
    }
}
