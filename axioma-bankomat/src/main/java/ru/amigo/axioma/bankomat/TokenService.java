package ru.amigo.axioma.bankomat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * @author denis.chernonozhkin
 * @since 21.10.2026 0:03
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final RestClient coreRestClient;
    private String cachedToken;

    public TokenService() {
        this.coreRestClient = RestClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    public synchronized String getToken() {
        if (cachedToken == null) {
            // Логинимся в ядро
            Map<String, String> loginRequest = Map.of(
                    "username", "atm_service",
                    "password", "atm_secret"
            );

            Map<String, Object> response = coreRestClient.post()
                    .uri("/api/auth/login")
                    .body(loginRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("token") && response.get("token") != null) {
                cachedToken = (String) response.get("token");
                log.info("Bankomat: токен получен");
            } else {
                log.error("Bankomat: не удалось получить токен");
                throw new RuntimeException("Не удалось получить токен для банкомата");
            }
        }
        return cachedToken;
    }
}
