package ru.amigo.axioma.deposit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.amigo.axioma.deposit.dto.AtmDepositRequest;
import ru.amigo.axioma.deposit.dto.DepositResponse;

import java.net.URI;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 22:45
 */
@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM API", description = "API для взаимодействия с банкоматами")
public class AtmController {

    private final DepositService depositService;

    public AtmController(DepositService depositService) {
        this.depositService = depositService;
    }

    @Operation(summary = "Обработать внесение наличных", description = "Вызывается банкоматом при внесении купюр")
    @PostMapping("/deposits")
    public ResponseEntity<DepositResponse> processDeposit(
            @Valid @RequestBody AtmDepositRequest request
    ) {
        DepositResponse response = depositService.processAtmDeposit(
                request.accountId(),
                request.amount(),
                request.atmId(),
                request.traceNumber()
        );
        return ResponseEntity
                .created(URI.create("/api/atm/deposits/" + response.id()))
                .body(response);
    }
}
