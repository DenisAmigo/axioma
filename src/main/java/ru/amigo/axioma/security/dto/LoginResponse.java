package ru.amigo.axioma.security.dto;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 20:25
 */
public record LoginResponse(
        String token,
        String type,
        long expiresInMs
) {
    public static LoginResponse of(String token, long expiresInMs) {
        return new LoginResponse(token, "Bearer", expiresInMs);
    }
}
