package ru.amigo.axioma.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 20:09
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {}
