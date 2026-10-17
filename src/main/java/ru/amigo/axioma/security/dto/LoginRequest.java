package ru.amigo.axioma.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 20:25
 */
public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {}
