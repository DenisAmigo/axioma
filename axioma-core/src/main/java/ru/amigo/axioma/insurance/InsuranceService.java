package ru.amigo.axioma.insurance;

import org.springframework.stereotype.Service;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 19:02
 */
@Service
public class InsuranceService {

    private final InsuranceQuoteClient quoteClient;

    public InsuranceService(InsuranceQuoteClient quoteClient) {
        this.quoteClient = quoteClient;
    }

    public List<InsuranceQuoteResponse> getQuotes(InsuranceQuoteRequest request) {
        try {
            // Ждем результат максимум 3 секунды
            CompletableFuture<List<InsuranceQuoteResponse>> future = quoteClient.getQuotesAsync(request);

            // Если страховая не ответит за 3 секунды, мы отдадим пустой список (таймаут)
            List<InsuranceQuoteResponse> responses = future.orTimeout(3, java.util.concurrent.TimeUnit.SECONDS).join();

            // Сортируем по цене (от дешевой к дорогой)
            if (responses != null) {
                return responses.stream()
                        .sorted((a, b) -> a.amount().compareTo(b.amount()))
                        .toList();
            }
            return List.of();

        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить расчет ОСАГО", e);
        }
    }
}
