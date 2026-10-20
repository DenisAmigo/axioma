package ru.amigo.axioma.account.dto;

import jakarta.validation.constraints.NotNull;
import ru.amigo.axioma.account.Currency;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:40
 */
public record OpenAccountRequest(
        @NotNull(message = "UserId обязателен")
        Long userId,

        @NotNull(message = "Валюта обязательна")
        Currency currency
) {}
