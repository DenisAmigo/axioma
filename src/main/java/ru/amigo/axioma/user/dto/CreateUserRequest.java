package ru.amigo.axioma.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 2:19
 */
public record CreateUserRequest(
        @NotBlank(message = "Требуется имя пользователя")
        @Size(min = 3, max = 50, message = "Длина имени пользователя должна составлять от 3 до 50 символов")
        String username,

        @NotBlank(message = "Требуется электронная почта")
        @Email(message = "Адрес электронной почты должен быть корректным")
        @Size(max = 255, message = "Длина адреса электронной почты не должна превышать 255 символов")
        String email,

        @NotBlank(message = "Требуется ввести пароль")
        @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
        String password,

        @Size(max = 255, message = "Полное имя не должно превышать 255 символов")
        String fullName
) {}
