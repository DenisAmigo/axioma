package ru.amigo.axioma.deposit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;
import ru.amigo.axioma.account.AccountStatus;
import ru.amigo.axioma.account.Currency;
import ru.amigo.axioma.account.exception.AccountNotFoundException;
import ru.amigo.axioma.deposit.dto.DepositResponse;
import ru.amigo.axioma.ledger.LedgerEntryRepository;
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
 * @since 21.10.2026 2:28
 */
@ExtendWith(MockitoExtension.class)
public class DepositServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DepositTransactionRepository depositRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private DepositService depositService;

    private Account account;
    private final String traceNumber = "trace-123";

    @BeforeEach
    void setUp() {
        User user = new User("ivanov", "ivanov@example.com", "hash", "Ivan Ivanov");
        setId(user, 1L);

        account = new Account(user, "40817000000000000001", Currency.RUB);
        setId(account, 10L);
        account.setBalance(new BigDecimal("1000.00"));
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
    @DisplayName("Депозит: успех")
    void processAtmDeposit_shouldCreditAccount_whenDataIsValid() {
        // given
        when(depositRepository.findByIdempotencyKey(traceNumber)).thenReturn(Optional.empty());
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(depositRepository.save(any(DepositTransaction.class))).thenAnswer(i -> {
            DepositTransaction tx = i.getArgument(0);
            setId(tx, 100L);
            return tx;
        });

        // when
        DepositResponse response = depositService.processAtmDeposit(10L, new BigDecimal("500.00"), "ATM-1", traceNumber);

        // then
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(0, new BigDecimal("1500.00").compareTo(account.getBalance()));
        verify(accountRepository).save(account);
        verify(ledgerEntryRepository).save(any());
    }

    @Test
    @DisplayName("Депозит: идемпотентность (повторный traceNumber)")
    void processAtmDeposit_shouldReturnExisting_whenTraceNumberExists() {
        // given
        DepositTransaction existingTx = new DepositTransaction(account, new BigDecimal("500.00"), Currency.RUB);
        setId(existingTx, 100L);
        when(depositRepository.findByIdempotencyKey(traceNumber)).thenReturn(Optional.of(existingTx));

        // when
        DepositResponse response = depositService.processAtmDeposit(10L, new BigDecimal("500.00"), "ATM-1", traceNumber);

        // then
        assertNotNull(response);
        assertEquals(100L, response.id());
        // Баланс не изменился
        assertEquals(0, new BigDecimal("1000.00").compareTo(account.getBalance()));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Депозит: счёт не найден")
    void processAtmDeposit_shouldThrowException_whenAccountNotFound() {
        // given
        when(depositRepository.findByIdempotencyKey(traceNumber)).thenReturn(Optional.empty());
        when(accountRepository.findById(10L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(AccountNotFoundException.class,
                () -> depositService.processAtmDeposit(10L, new BigDecimal("500.00"), "ATM-1", traceNumber));
    }

    @Test
    @DisplayName("Депозит: счёт заблокирован")
    void processAtmDeposit_shouldThrowException_whenAccountIsBlocked() {
        // given
        account.setStatus(AccountStatus.FROZEN);
        when(depositRepository.findByIdempotencyKey(traceNumber)).thenReturn(Optional.empty());
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> depositService.processAtmDeposit(10L, new BigDecimal("500.00"), "ATM-1", traceNumber));
    }
}
