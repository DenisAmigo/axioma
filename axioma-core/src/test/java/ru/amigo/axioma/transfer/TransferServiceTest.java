package ru.amigo.axioma.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.AccountStatus;
import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.account.exception.AccountAccessDeniedException;
import ru.amigo.axioma.idempotency.IdempotencyKey;
import ru.amigo.axioma.idempotency.IdempotencyKeyService;
import ru.amigo.axioma.transfer.dto.CreateTransferRequest;
import ru.amigo.axioma.transfer.dto.TransferResponse;
import ru.amigo.axioma.transfer.exception.AccountNotActiveException;
import ru.amigo.axioma.transfer.exception.CurrencyMismatchException;
import ru.amigo.axioma.transfer.exception.InsufficientFundsException;
import ru.amigo.axioma.transfer.exception.SameAccountTransferException;
import ru.amigo.axioma.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author denis.chernonozhkin
 * @since 18.10.2026 0:03
 */
@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyKeyService idempotencyKeyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private TransferService transferService;

    private User ivanov;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        ivanov = new User("ivanov", "ivanov@example.com", "hash", "Ivan Ivanov");
        setId(ivanov, 1L);

        User petrov = new User("petrov", "petrov@example.com", "hash", "Petr Petrov");
        setId(petrov, 2L);

        fromAccount = new Account(ivanov, "40817000000000000001", Currency.RUB);
        setId(fromAccount, 10L);
        fromAccount.setBalance(new BigDecimal("1000.00"));

        toAccount = new Account(petrov, "40817000000000000002", Currency.RUB);
        setId(toAccount, 20L);
        toAccount.setBalance(new BigDecimal("500.00"));
    }

    private void setId(Object entity, Long id) {
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String hashRequest(Long fromId, Long toId, String amount, String description) {
        try {
            String raw = fromId + "|" + toId + "|" + amount + "|" + (description == null ? "" : description);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Перевод: успех")
    void create_shouldTransferMoney_whenAllValid() {
        // given
        CreateTransferRequest request = new CreateTransferRequest(10L, 20L, new BigDecimal("150.00"), "Обед");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

        // Мокаем сохранение: возвращаем переданный объект, чтобы TransferResponse.from сработал
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            setId(transfer, 100L);
            return transfer;
        });

        // when
        TransferResponse response = transferService.create(request, 1L, null);

        // then
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(new BigDecimal("850.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("650.00"), toAccount.getBalance());
        assertEquals(0, response.amount().compareTo(new BigDecimal("150.00")));
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Перевод: идемпотентность - повторный запрос возвращает сохраненный ответ")
    void create_shouldReturnSavedResponse_whenIdempotencyKeyExists() throws Exception {
        // given
        String idempotencyKey = "test-key-123";
        CreateTransferRequest request = new CreateTransferRequest(10L, 20L, new BigDecimal("150.00"), "Обед");

        // Эмулируем, что ключ уже есть в БД с сохраненным ответом
        TransferResponse savedResponse = new TransferResponse(
                100L, 10L, 20L, new BigDecimal("150.00"), Currency.RUB,
                TransferStatus.COMPLETED, "Обед", null,
                Instant.now(), Instant.now()
        );
        String savedJson = objectMapper.writeValueAsString(savedResponse);

        IdempotencyKey existingKey = new IdempotencyKey(
                idempotencyKey, ivanov, "hash", Instant.now().plusSeconds(3600)
        );
        existingKey.setResponseBody(savedJson);
        existingKey.setResponseStatus(201);
        existingKey.setRequestHash(hashRequest(10L, 20L, "150.00", "Обед"));

        // Хеш запроса должен совпадать с тем, что сервис вычислит внутри
        // (в реальном коде хеш считается от fromAccountId|toAccountId|amount|description)
        // Для упрощения замокаем, что сервис найдет ключ:
        when(idempotencyKeyService.findByKeyAndUser(idempotencyKey, 1L))
                .thenReturn(Optional.of(existingKey));

        // when
        TransferResponse response = transferService.create(request, 1L, idempotencyKey);

        // then
        assertNotNull(response);
        assertEquals(100L, response.id());
        // Балансы не должны измениться
        assertEquals(new BigDecimal("1000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), toAccount.getBalance());
        // save не должен вызываться (перевод не выполняется повторно)
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод: ошибка - недостаточно средств")
    void create_shouldThrowException_whenInsufficientFunds() {
        // given
        CreateTransferRequest request = new CreateTransferRequest(10L, 20L, new BigDecimal("2000.00"), "Обед");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

        // when & then
        assertThrows(InsufficientFundsException.class, () -> transferService.create(request, 1L, null));
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод: ошибка - тот же счёт")
    void create_shouldThrowException_whenSameAccount() {
        // given
        CreateTransferRequest request = new CreateTransferRequest(10L, 10L, new BigDecimal("100.00"), "Обед");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));

        // when & then
        assertThrows(SameAccountTransferException.class, () -> transferService.create(request, 1L, null));
    }

    @Test
    @DisplayName("Перевод: ошибка - разные валюты")
    void create_shouldThrowException_whenCurrencyMismatch() {
        // given
        toAccount.setCurrency(Currency.USD);
        CreateTransferRequest request = new CreateTransferRequest(10L, 20L, new BigDecimal("100.00"), "Обед");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

        // when & then
        assertThrows(CurrencyMismatchException.class, () -> transferService.create(request, 1L, null));
    }

    @Test
    @DisplayName("Перевод: ошибка - счёт заблокирован")
    void create_shouldThrowException_whenAccountIsBlocked() {
        // given
        fromAccount.setStatus(AccountStatus.FROZEN);
        CreateTransferRequest request = new CreateTransferRequest(10L, 20L, new BigDecimal("100.00"), "Обед");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(toAccount));

        // when & then
        assertThrows(AccountNotActiveException.class, () -> transferService.create(request, 1L, null));
    }

    @Test
    @DisplayName("Перевод: ошибка - чужой счёт")
    void create_shouldThrowException_whenFromAccountBelongsToAnotherUser() {
        // given
        CreateTransferRequest request = new CreateTransferRequest(10L, 20L, new BigDecimal("100.00"), "Обед");
        when(accountRepository.findById(10L)).thenReturn(Optional.of(fromAccount));

        // when & then
        assertThrows(AccountAccessDeniedException.class, () -> transferService.create(request, 999L, null));
    }
}
