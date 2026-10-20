package ru.amigo.axioma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 20:51
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient bankomatRestClient() {
        // Настраиваем таймауты: 3 секунды на подключение и 3 секунды на чтение ответа
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl("http://localhost:8081")
                .requestFactory(factory)
                .build();
    }
}
