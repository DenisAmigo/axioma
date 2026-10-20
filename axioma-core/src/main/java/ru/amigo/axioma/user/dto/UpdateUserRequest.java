package ru.amigo.axioma.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 2:22
 */
public record UpdateUserRequest(
        @Email(message = "Адрес электронной почты должен быть корректным")
        @Size(max = 255, message = "Длина адреса электронной почты не должна превышать 255 символов")
        String email,

        @Size(max = 255, message = "Полное имя не должно превышать 255 символов")
        String fullName
) {}
