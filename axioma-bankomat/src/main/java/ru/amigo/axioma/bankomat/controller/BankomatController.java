package ru.amigo.axioma.bankomat.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import ru.amigo.axioma.bankomat.TokenService;
import ru.amigo.axioma.bankomat.dto.BankomatRequest;

import java.util.Map;
import java.util.UUID;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 23:29
 */
@RestController
@RequestMapping("/api/atm")
public class BankomatController {

    private static final Logger log = LoggerFactory.getLogger(BankomatController.class);

    private final RestClient coreRestClient;
    private final TokenService tokenService;

    public BankomatController(TokenService tokenService) {
        this.tokenService = tokenService;
        this.coreRestClient = RestClient.builder()
                .baseUrl("http://localhost:8080") // Ядро
                .build();
    }

    @PostMapping("/initiate-deposit")
    @CircuitBreaker(name = "coreService")
    @Retry(name = "coreService", fallbackMethod = "fallbackDeposit")
    public Map<String, Object> initiateDeposit(@RequestBody BankomatRequest request) {
        // Иммитируем сбой
        if (request.simulateFailure()) {
            log.warn("Bankomat: Смоделирован сбой при приеме купюр!");
            return Map.of(
                    "success", false,
                    "message", "Ошибка банкомата: купюры зажевало. Обратитесь в поддержку."
            );
        }

        // Банкомат формирует сообщение (traceNumber)
        String traceNumber = UUID.randomUUID().toString();
        String atmId = "ATM-MSK-001";

        log.info("Bankomat: Попытка отправить депозит в ядро. Trace: {}", traceNumber);

        // Отправляем в Ядро с помощью POST
        String token = tokenService.getToken();

        Map<String, Object> coreRequest = Map.of(
                "accountId", request.accountId(),
                "amount", request.amount(),
                "atmId", atmId,
                "traceNumber", traceNumber
        );

        Map<String, Object> coreResponse = coreRestClient.post()
                .uri("/api/atm/deposits")
                .header("Authorization", "Bearer " + token)
                .body(coreRequest)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        // Формируем ответ для фронтенда
        if (coreResponse != null && "CONFIRMED".equals(coreResponse.get("status"))) {
            return Map.of(
                    "success", true,
                    "transactionId", coreResponse.get("atmTransactionId"),
                    "message", "Купюры приняты. Баланс пополнен."
            );
        } else {
            return Map.of(
                    "success", false,
                    "message", "Ядро не подтвердило транзакцию"
            );
        }
    }

    public Map<String, Object> fallbackDeposit(BankomatRequest request, Throwable t) {
        log.error("Bankomat: Все попытки исчерпаны. Ошибка: {}", t.getMessage());
        return Map.of(
                "success", false,
                "message", "Сервис временно недоступен. Попробуйте позже."
        );
    }
}
