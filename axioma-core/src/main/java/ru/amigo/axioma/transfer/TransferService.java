package ru.amigo.axioma.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.AccountStatus;
import ru.amigo.axioma.account.exception.AccountAccessDeniedException;
import ru.amigo.axioma.account.exception.AccountNotFoundException;
import ru.amigo.axioma.idempotency.IdempotencyKey;
import ru.amigo.axioma.idempotency.IdempotencyKeyService;
import ru.amigo.axioma.ledger.EntryType;
import ru.amigo.axioma.ledger.LedgerEntry;
import ru.amigo.axioma.outbox.OutboxService;
import ru.amigo.axioma.outbox.event.TransferCompletedEvent;
import ru.amigo.axioma.transfer.dto.CreateTransferRequest;
import ru.amigo.axioma.transfer.dto.TransferResponse;
import ru.amigo.axioma.transfer.exception.AccountNotActiveException;
import ru.amigo.axioma.transfer.exception.CurrencyMismatchException;
import ru.amigo.axioma.transfer.exception.InsufficientFundsException;
import ru.amigo.axioma.transfer.exception.SameAccountTransferException;
import ru.amigo.axioma.transfer.exception.TransferNotFoundException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 17:20
 */
@Service
@Transactional(readOnly = true)
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final IdempotencyKeyService idempotencyKeyService;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            IdempotencyKeyService idempotencyKeyService,
            ObjectMapper objectMapper,
            OutboxService outboxService
    ) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.idempotencyKeyService = idempotencyKeyService;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    @Transactional
    public TransferResponse create(CreateTransferRequest request, Long userId, String idempotencyKey) {
        // 1. Проверка идемпотентности
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestHash = hashRequest(request);
            Optional<IdempotencyKey> existing = idempotencyKeyService.findByKeyAndUser(idempotencyKey, userId);
            if (existing.isPresent()) {
                IdempotencyKey key = existing.get();
                if (!key.getRequestHash().equals(requestHash)) {
                    throw new IllegalStateException("Idempotency-Key уже использован с другим телом запроса");
                }
                if (key.getResponseBody() != null) {
                    try {
                        return objectMapper.readValue(key.getResponseBody(), TransferResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Не удалось прочитать сохранённый ответ", e);
                    }
                }
            }
        }

        // 2. Загрузка и валидация
        Account from = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.fromAccountId()));
        if (!from.getUser().getId().equals(userId)) {
            throw new AccountAccessDeniedException(from.getId());
        }
        Account to = accountRepository.findById(request.toAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.toAccountId()));

        if (from.getId().equals(to.getId())) {
            throw new SameAccountTransferException(from.getId());
        }
        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(from.getId(), from.getStatus().name());
        }
        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(to.getId(), to.getStatus().name());
        }
        if (from.getCurrency() != to.getCurrency()) {
            throw new CurrencyMismatchException(from.getCurrency().name(), to.getCurrency().name());
        }
        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(from.getId(), request.amount(), from.getBalance());
        }

        // 3. Выполнение перевода
        BigDecimal amount = request.amount();

        Transfer transfer = new Transfer(from, to, amount, from.getCurrency());
        transfer.setDescription(request.description());
        transfer.setIdempotencyKey(idempotencyKey);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        LedgerEntry debit = new LedgerEntry(transfer, from, EntryType.DEBIT, amount, from.getBalance());
        LedgerEntry credit = new LedgerEntry(transfer, to, EntryType.CREDIT, amount, to.getBalance());
        transfer.addLedgerEntry(debit);
        transfer.addLedgerEntry(credit);

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());

        Transfer saved = transferRepository.save(transfer);
        TransferResponse response = TransferResponse.from(saved);

        // 4. Сохранение события в outbox
        outboxService.saveEvent(
            "TRANSFER",
            saved.getId(),
            "TransferCompleted",
            new TransferCompletedEvent(
                saved.getId(),
                saved.getFromAccount().getId(),
                saved.getToAccount().getId(),
                saved.getAmount(),
                saved.getCurrency()
            )
        );

        // 5. Сохранение ответа для идемпотентности
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                idempotencyKeyService.save(
                        idempotencyKey,
                        userId,
                        hashRequest(request),
                        responseJson,
                        201
                );
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Не удалось сериализовать ответ", e);
            }
        }

        return response;
    }

    public TransferResponse getById(Long id, Long userId) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(id));
        if (!transfer.getFromAccount().getUser().getId().equals(userId)
                && !transfer.getToAccount().getUser().getId().equals(userId)) {
            throw new TransferNotFoundException(id);
        }
        return TransferResponse.from(transfer);
    }

    public Page<TransferResponse> listByUser(Long userId, Pageable pageable) {
        return transferRepository.findByFromAccountUserId(userId, pageable)
                .map(TransferResponse::from);
    }

    private String hashRequest(CreateTransferRequest request) {
        try {
            String raw = request.fromAccountId() + "|" +
                    request.toAccountId() + "|" +
                    request.amount().toPlainString() + "|" +
                    (request.description() == null ? "" : request.description());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
