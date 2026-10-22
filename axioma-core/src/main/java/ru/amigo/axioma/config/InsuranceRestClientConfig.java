package ru.amigo.axioma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:55
 */
@Configuration
public class InsuranceRestClientConfig {

    @Bean
    public RestClient insuranceRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        return RestClient.builder()
                .baseUrl("http://localhost:8082")
                .requestFactory(factory)
                .build();
    }
}
