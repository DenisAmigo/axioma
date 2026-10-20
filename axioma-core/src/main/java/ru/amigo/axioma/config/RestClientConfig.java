package ru.amigo.axioma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 20:51
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient bankomatRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }
}
