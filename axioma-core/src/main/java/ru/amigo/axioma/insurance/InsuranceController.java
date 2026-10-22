package ru.amigo.axioma.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteResponse;

import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 19:05
 */
@RestController
@RequestMapping("/api/insurance")
@Tag(name = "Страхование", description = "Расчет и оформление ОСАГО")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @Operation(summary = "Получить расчет ОСАГО", description = "Возвращает предложения от страховых компаний, отсортированные по цене")
    @PostMapping("/quotes")
    public ResponseEntity<List<InsuranceQuoteResponse>> getQuotes(@RequestBody InsuranceQuoteRequest request) {
        return ResponseEntity.ok(insuranceService.getQuotes(request));
    }
}
