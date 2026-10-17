package ru.amigo.axioma.transfer.exception;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:01
 */
public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String from, String to) {
        super("Валюты счетов не совпадают: " + from + " → " + to);
    }
}
