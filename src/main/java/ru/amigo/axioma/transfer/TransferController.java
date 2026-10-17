package ru.amigo.axioma.transfer;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.security.CustomUserDetails;
import ru.amigo.axioma.transfer.dto.CreateTransferRequest;
import ru.amigo.axioma.transfer.dto.TransferResponse;

import java.net.URI;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:28
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @Valid @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        TransferResponse response = transferService.create(request, user.getId(), idempotencyKey);
        return ResponseEntity
                .created(URI.create("/api/transfers/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(transferService.getById(id, user.getId()));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<TransferResponse>> myTransfers(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(transferService.listByUser(user.getId(), pageable));
    }
}
