package ru.amigo.axioma.transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.idempotency.IdempotencyKeyRepository;
import ru.amigo.axioma.ledger.LedgerEntryRepository;
import ru.amigo.axioma.transfer.dto.CreateTransferRequest;
import ru.amigo.axioma.transfer.dto.TransferResponse;
import ru.amigo.axioma.user.Role;
import ru.amigo.axioma.user.RoleRepository;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author denis.chernonozhkin
 * @since 18.10.2026 0:53
 */
@SpringBootTest
@Testcontainers
class TransferServiceIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private User ivanov;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        // Очистка БД (порядок важен из-за FK)
        idempotencyKeyRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Создаём роль, если её нет
        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new Role("CLIENT", "Клиент")));

        ivanov = new User("ivanov", "ivanov@example.com", "hash", "Ivan Ivanov");
        ivanov.addRole(clientRole);
        ivanov = userRepository.save(ivanov);

        User petrov = new User("petrov", "petrov@example.com", "hash", "Petr Petrov");
        petrov.addRole(clientRole);
        petrov = userRepository.save(petrov);

        fromAccount = new Account(ivanov, "40817000000000000001", Currency.RUB);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount = accountRepository.save(fromAccount);

        toAccount = new Account(petrov, "40817000000000000002", Currency.RUB);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount = accountRepository.save(toAccount);
    }

    @Test
    @DisplayName("Интеграционный: успешный перевод с проводками")
    void shouldTransferMoneyAndCreateLedgerEntries() {
        // given
        CreateTransferRequest request = new CreateTransferRequest(
                fromAccount.getId(), toAccount.getId(), new BigDecimal("150.00"), "Обед"
        );

        // when
        TransferResponse response = transferService.create(request, ivanov.getId(), null);

        // then
        assertNotNull(response);
        assertEquals(TransferStatus.COMPLETED, response.status());

        // Проверяем балансы
        Account updatedFrom = accountRepository.findById(fromAccount.getId()).orElseThrow();
        Account updatedTo = accountRepository.findById(toAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("850.00").compareTo(updatedFrom.getBalance()));
        assertEquals(0, new BigDecimal("650.00").compareTo(updatedTo.getBalance()));

        // Проверяем леджер
        var entries = ledgerEntryRepository.findByTransfer(
                transferRepository.findById(response.id()).orElseThrow()
        );
        assertEquals(2, entries.size());

        // Проверяем, что версии счетов изменились (оптимистичная блокировка)
        assertTrue(updatedFrom.getVersion() > 0);
        assertTrue(updatedTo.getVersion() > 0);
    }

    @Test
    @DisplayName("Интеграционный: идемпотентность - повторный запрос возвращает сохраненный ответ")
    void shouldReturnSameResponseForSameIdempotencyKey() {
        // given
        String idempotencyKey = "test-key-it-123";
        CreateTransferRequest request = new CreateTransferRequest(
                fromAccount.getId(), toAccount.getId(), new BigDecimal("150.00"), "Обед"
        );

        // Первый запрос — выполняет перевод
        TransferResponse firstResponse = transferService.create(request, ivanov.getId(), idempotencyKey);
        assertNotNull(firstResponse);
        assertEquals(TransferStatus.COMPLETED, firstResponse.status());

        // Запоминаем балансы после первого перевода
        BigDecimal balanceAfterFirst = accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance();

        // Второй запрос — должен вернуть тот же ответ, не выполняя перевод повторно
        TransferResponse secondResponse = transferService.create(request, ivanov.getId(), idempotencyKey);

        // then
        assertNotNull(secondResponse);
        assertEquals(firstResponse.id(), secondResponse.id(), "ID перевода должен совпадать");
        assertEquals(firstResponse.amount(), secondResponse.amount());

        // Баланс НЕ должен измениться
        BigDecimal balanceAfterSecond = accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance();
        assertEquals(balanceAfterFirst, balanceAfterSecond, "Баланс не должен измениться при повторном запросе");

        // В БД должен быть только один перевод
        assertEquals(1, transferRepository.count(), "Должен быть только один перевод");

        // И две проводки (DEBIT + CREDIT)
        assertEquals(2, ledgerEntryRepository.count(), "Должно быть две проводки");

        // Ключ идемпотентности сохранён
        assertTrue(idempotencyKeyRepository.existsById(idempotencyKey));
    }
}
