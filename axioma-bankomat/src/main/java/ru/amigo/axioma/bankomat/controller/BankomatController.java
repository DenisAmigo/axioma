package ru.amigo.axioma.bankomat.controller;

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

    private final RestClient coreRestClient;
    private final TokenService tokenService;

    public BankomatController(TokenService tokenService) {
        this.tokenService = tokenService;
        this.coreRestClient = RestClient.builder()
                .baseUrl("http://localhost:8080") // Ядро
                .build();
    }

    @PostMapping("/initiate-deposit")
    public Map<String, Object> initiateDeposit(@RequestBody BankomatRequest request) {
        // Банкомат формирует сообщение (traceNumber)
        String traceNumber = UUID.randomUUID().toString();
        String atmId = "ATM-MSK-001";

        System.out.println("Bankomat: Принимаю купюры на " + request.amount() + " руб.");

        // Отправляем в Ядро с помощью POST
        try {
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
                    .body(new ParameterizedTypeReference<>() {});

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

        } catch (Exception e) {
            System.err.println("Bankomat: Ошибка связи с ядром: " + e.getMessage());
            return Map.of("success", false, "message", "Ошибка процессинга: " + e.getMessage());
        }
    }
}
