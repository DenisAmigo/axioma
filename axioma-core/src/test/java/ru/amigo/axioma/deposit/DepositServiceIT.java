package ru.amigo.axioma.deposit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.deposit.dto.DepositResponse;
import ru.amigo.axioma.ledger.LedgerEntryRepository;
import ru.amigo.axioma.user.Role;
import ru.amigo.axioma.user.RoleRepository;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author denis.chernonozhkin
 * @since 21.10.2026 9:35
 */
@SpringBootTest
@Testcontainers
public class DepositServiceIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DepositService depositService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepositTransactionRepository depositRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        depositRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new Role("CLIENT", "Клиент")));

        User ivanov = new User("ivanov", "ivanov@example.com", "hash", "Ivan");
        ivanov.addRole(clientRole);
        ivanov = userRepository.save(ivanov);

        account = new Account(ivanov, "40817000000000000001", Currency.RUB);
        account.setBalance(new BigDecimal("1000.00"));
        account = accountRepository.save(account);
    }

    @Test
    @DisplayName("Интеграционный: успешное внесение наличных")
    void shouldProcessDepositAndCreateLedgerEntry() {
        // when
        DepositResponse response = depositService.processAtmDeposit(
                account.getId(), new BigDecimal("500.00"), "ATM-1", "trace-123"
        );

        // then
        assertNotNull(response);
        assertEquals(DepositStatus.CONFIRMED, response.status());

        // Проверяем баланс
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1500.00").compareTo(updatedAccount.getBalance()));

        // Проверяем леджер
        var entries = ledgerEntryRepository.findAll();
        assertEquals(1, entries.size());
        assertNull(entries.getFirst().getTransfer()); // Важно: transfer должен быть null для депозита
        assertEquals(0, new BigDecimal("500.00").compareTo(entries.getFirst().getAmount()));
    }

    @Test
    @DisplayName("Интеграционный: идемпотентность по traceNumber")
    void shouldNotDuplicateDepositWhenSameTraceNumber() {
        // Первый раз
        depositService.processAtmDeposit(account.getId(), new BigDecimal("500.00"), "ATM-1", "trace-999");

        // Второй раз
        depositService.processAtmDeposit(account.getId(), new BigDecimal("500.00"), "ATM-1", "trace-999");

        // then
        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1500.00").compareTo(updatedAccount.getBalance())); // Баланс изменился только 1 раз
        assertEquals(1, depositRepository.count()); // В БД только 1 транзакция
        assertEquals(1, ledgerEntryRepository.count()); // И только 1 запись в леджере
    }
}
