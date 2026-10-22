package ru.amigo.axioma.insurance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.insurance.dto.CreatePolicyRequest;
import ru.amigo.axioma.insurance.dto.PolicyResponse;
import ru.amigo.axioma.insurance.exception.InsufficientAvailableFundsException;
import ru.amigo.axioma.user.User;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 23:24
 */
@ExtendWith(MockitoExtension.class)
public class InsuranceServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InsurancePolicyRepository policyRepository;

    @InjectMocks
    private InsuranceService insuranceService;

    private Account account;

    @BeforeEach
    void setUp() {
        User user = new User("ivanov", "ivanov@example.com", "hash", "Ivan Ivanov");
        setId(user, 1L);

        account = new Account(user, "40817000000000000001", Currency.RUB);
        setId(account, 10L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setHeldAmount(new BigDecimal("200.00"));
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

    @Test
    @DisplayName("Создание заявки: успех")
    void createPolicy_shouldHoldFunds_whenBalanceIsSufficient() {
        // given
        CreatePolicyRequest request = new CreatePolicyRequest(10L, "ALPHA", new BigDecimal("500.00"));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(i -> {
            InsurancePolicy p = i.getArgument(0);
            setId(p, 100L);
            return p;
        });

        // when
        PolicyResponse response = insuranceService.createPolicy(1L, request);

        // then
        assertNotNull(response);
        assertEquals(PolicyStatus.PENDING, response.status());
        assertEquals(0, new BigDecimal("700.00").compareTo(account.getHeldAmount()));
    }

    @Test
    @DisplayName("Создание заявки: ошибка - недостаточно доступных средств")
    void createPolicy_shouldThrowException_whenInsufficientFunds() {
        // given
        // Доступно 800, просим 900
        CreatePolicyRequest request = new CreatePolicyRequest(10L, "ALPHA", new BigDecimal("900.00"));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));

        // when & then
        assertThrows(InsufficientAvailableFundsException.class,
                () -> insuranceService.createPolicy(1L, request));
    }

    @Test
    @DisplayName("Создание заявки: ошибка - чужой счет")
    void createPolicy_shouldThrowException_whenAccountBelongsToAnotherUser() {
        // given
        CreatePolicyRequest request = new CreatePolicyRequest(10L, "ALPHA", new BigDecimal("500.00"));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(account));

        // when & then
        assertThrows(RuntimeException.class,
                () -> insuranceService.createPolicy(999L, request));
    }
}
