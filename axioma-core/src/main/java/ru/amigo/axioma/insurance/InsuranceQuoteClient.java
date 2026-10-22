package ru.amigo.axioma.insurance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 18:59
 */
@Service
public class InsuranceQuoteClient {

    public static final Logger log = LoggerFactory.getLogger(InsuranceQuoteClient.class);
    private final RestClient insuranceRestClient;

    public InsuranceQuoteClient(RestClient insuranceRestClient) {
        this.insuranceRestClient = insuranceRestClient;
    }

    public CompletableFuture<List<InsuranceQuoteResponse>> getQuotesAsync(InsuranceQuoteRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Ядро: отправляем запрос на расчет ОСАГО в axioma-insurance...");
            try {
                return insuranceRestClient.post()
                        .uri("/api/insurance/quotes")
                        .body(request)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
            } catch (Exception e) {
                log.error("Ядро: Ошибка при запросе к страховой: {}", e.getMessage());
                // Если страховая упала полностью, возвращаем пустой список
                return List.of();
            }
        });
    }
}
