package ru.amigo.axioma.bankomat.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.bankomat.dto.AtmDepositRequest;
import ru.amigo.axioma.bankomat.dto.AtmDepositResponse;

import java.util.UUID;

/**
 * @author denis.chernonozhkin
 * @since 20.10.2026 20:45
 */
@RestController
@RequestMapping("/api/atm")
public class AtmController {

    @PostMapping("/deposit")
    public AtmDepositResponse deposit(@RequestBody AtmDepositRequest request) {
        // Имитация: банкомат принял купюры
        System.out.println("ATM received deposit: " + request.amount() + " to account " + request.accountNumber());

        return new AtmDepositResponse(
                true,
                UUID.randomUUID().toString(),
                "Купюры приняты"
        );
    }
}
