package ru.amigo.axioma.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.insurance.dto.CreatePolicyRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteRequest;
import ru.amigo.axioma.insurance.dto.InsuranceQuoteResponse;
import ru.amigo.axioma.insurance.dto.PolicyResponse;
import ru.amigo.axioma.security.CustomUserDetails;

import java.net.URI;
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
    public ResponseEntity<List<InsuranceQuoteResponse>> getQuotes(
            @RequestBody InsuranceQuoteRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(insuranceService.getQuotes(user.getId(), request));
    }

    @Operation(summary = "Создать заявку на ОСАГО", description = "Замораживает деньги на счете и создает заявку (холд)")
    @PostMapping("/policies")
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PolicyResponse response = insuranceService.createPolicy(user.getId(), request);
        return ResponseEntity
                .created(URI.create("/api/insurance/policies/" + response.id()))
                .body(response);
    }

    @Operation(summary = "Подтвердить полис", description = "Списывает замороженные деньги и выпускает полис")
    @PostMapping("/policies/{id}/confirm")
    public ResponseEntity<PolicyResponse> confirmPolicy(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(insuranceService.confirmPolicy(user.getId(), id));
    }
}
