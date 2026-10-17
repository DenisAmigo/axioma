package ru.amigo.axioma.account;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.account.dto.AccountResponse;
import ru.amigo.axioma.account.dto.OpenAccountRequest;

import java.net.URI;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 13:53
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        AccountResponse response = accountService.open(request);
        return ResponseEntity
                .created(URI.create("/api/accounts/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(accountService.getById(id, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<AccountResponse>> myAccounts(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(accountService.listByUser(userId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> close(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        accountService.close(id, userId);
        return ResponseEntity.noContent().build();
    }
}
