package ru.amigo.axioma.insurance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.insurance.dto.CreatePolicyRequest;
import ru.amigo.axioma.insurance.dto.IssuePolicyResponse;
import ru.amigo.axioma.insurance.dto.PolicyResponse;
import ru.amigo.axioma.ledger.LedgerEntryRepository;
import ru.amigo.axioma.user.Role;
import ru.amigo.axioma.user.RoleRepository;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 23:29
 */
@SpringBootTest
@Testcontainers
public class InsuranceServiceIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InsuranceService insuranceService;

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PolicyExpirationScheduler scheduler;

    @MockitoBean
    private InsuranceQuoteClient quoteClient;

    private User ivanov;
    private Account account;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        Role clientRole = roleRepository.findByName("CLIENT").orElseGet(() -> roleRepository.save(new Role("CLIENT", "Клиент")));
        ivanov = new User("ivanov", "ivanov@example.com", "hash", "Ivan Ivanov");
        ivanov.addRole(clientRole);
        ivanov = userRepository.save(ivanov);

        account = new Account(ivanov, "40817000000000000001", Currency.RUB);
        account.setBalance(new BigDecimal("1000.00"));
        account = accountRepository.save(account);
    }

    @Test
    @DisplayName("Интеграционный: Сага - успешное подтверждение")
    void confirmPolicy_shouldDeductBalanceAndReleaseHold() {
        // 1. Создаем заявку
        CreatePolicyRequest request = new CreatePolicyRequest(account.getId(), "ALPHA", new BigDecimal("500.00"));
        PolicyResponse created = insuranceService.createPolicy(ivanov.getId(), request);
        assertEquals(PolicyStatus.PENDING, created.status());

        // 2. Мокаем успешный ответ страховой
        when(quoteClient.issuePolicy(any(), any())).thenReturn(new IssuePolicyResponse(true, "XXX1234567890", "OK"));

        // 3. Подтверждаем
        PolicyResponse confirmed = insuranceService.confirmPolicy(ivanov.getId(), created.id());

        // then
        assertEquals(PolicyStatus.ISSUED, confirmed.status());
        assertEquals("XXX1234567890", confirmed.policyNumber());

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(updatedAccount.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedAccount.getHeldAmount()));
    }

    @Test
    @DisplayName("Интеграционный: TTL - истечение заявки снимает холд")
    void scheduler_shouldExpirePolicyAndReleaseHold() {
        // 1. Создаем заявку
        CreatePolicyRequest request = new CreatePolicyRequest(account.getId(), "ALPHA", new BigDecimal("500.00"));
        PolicyResponse created = insuranceService.createPolicy(ivanov.getId(), request);

        // 2. Искусственно делаем её просроченной
        InsurancePolicy policy = policyRepository.findById(created.id()).orElseThrow();
        policy.setExpiresAt(Instant.now().minusSeconds(60));
        policyRepository.save(policy);

        // 3. Вызываем метод планировщика напрямую
        scheduler.expirePolicies();

        // then
        InsurancePolicy expired = policyRepository.findById(created.id()).orElseThrow();
        assertEquals(PolicyStatus.EXPIRED, expired.getStatus());

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedAccount.getHeldAmount()));
    }
}
