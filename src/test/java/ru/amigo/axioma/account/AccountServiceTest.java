package ru.amigo.axioma.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.amigo.axioma.account.dto.AccountResponse;
import ru.amigo.axioma.account.dto.OpenAccountRequest;
import ru.amigo.axioma.account.exception.AccountAccessDeniedException;
import ru.amigo.axioma.account.exception.AccountHasBalanceException;
import ru.amigo.axioma.account.exception.AccountNotFoundException;
import ru.amigo.axioma.user.User;
import ru.amigo.axioma.user.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 23:47
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User ivanov;
    private User petrov;
    private Account ivanovAccount;
    private Account petrovAccount;

    @BeforeEach
    void setUp() {
        ivanov = new User("ivanov", "ivanov@example.com", "hash", "Ivan Ivanov");
        setId(ivanov, 1L);

        petrov = new User("petrov", "petrov@example.com", "hash", "Petr Petrov");
        setId(petrov, 2L);

        ivanovAccount = new Account(ivanov, "40817000000000000001", Currency.RUB);
        setId(ivanovAccount, 10L);

        petrovAccount = new Account(petrov, "40817000000000000002", Currency.RUB);
        setId(petrovAccount, 20L);
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
    @DisplayName("Открытие счёта: успех")
    void open_shouldCreateAccount() {
        // given
        OpenAccountRequest request = new OpenAccountRequest(1L, Currency.RUB);
        when(userRepository.findById(1L)).thenReturn(Optional.of(ivanov));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            setId(acc, 10L);
            return acc;
        });

        // when
        AccountResponse response = accountService.open(request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals(Currency.RUB, response.currency());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.balance()));
        assertEquals(AccountStatus.ACTIVE, response.status());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Получение счёта: успех (свой счёт)")
    void getById_shouldReturnAccount_whenOwner() {
        // given
        when(accountRepository.findById(10L)).thenReturn(Optional.of(ivanovAccount));

        // when
        AccountResponse response = accountService.getById(10L, 1L);

        // then
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
    }

    @Test
    @DisplayName("Получение счёта: ошибка (чужой счёт)")
    void getById_shouldThrowException_whenNotOwner() {
        // given
        when(accountRepository.findById(20L)).thenReturn(Optional.of(petrovAccount));

        // when & then
        assertThrows(AccountAccessDeniedException.class,
                () -> accountService.getById(20L, 1L));
    }

    @Test
    @DisplayName("Получение счёта: ошибка (счёт не найден)")
    void getById_shouldThrowException_whenNotFound() {
        // given
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(AccountNotFoundException.class,
                () -> accountService.getById(999L, 1L));
    }

    @Test
    @DisplayName("Закрытие счёта: успех (баланс 0)")
    void close_shouldCloseAccount_whenBalanceIsZero() {
        // given
        ivanovAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(ivanovAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(ivanovAccount);

        // when
        accountService.close(10L, 1L);

        // then
        assertEquals(AccountStatus.CLOSED, ivanovAccount.getStatus());
        verify(accountRepository, times(1)).save(ivanovAccount);
    }

    @Test
    @DisplayName("Закрытие счёта: ошибка (ненулевой баланс)")
    void close_shouldThrowException_whenBalanceIsPositive() {
        // given
        petrovAccount.setBalance(new BigDecimal("100.00"));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(petrovAccount));

        // when & then
        assertThrows(AccountHasBalanceException.class,
                () -> accountService.close(20L, 2L));
        verify(accountRepository, never()).save(any());
    }
}
