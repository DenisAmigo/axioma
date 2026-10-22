package ru.amigo.axioma.insurance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.amigo.axioma.account.Account;
import ru.amigo.axioma.account.AccountRepository;

import java.time.Instant;
import java.util.List;

/**
 * @author denis.chernonozhkin
 * @since 22.10.2026 22:28
 */
@Component
public class PolicyExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PolicyExpirationScheduler.class);

    private final InsurancePolicyRepository policyRepository;
    private final AccountRepository accountRepository;

    public PolicyExpirationScheduler(InsurancePolicyRepository policyRepository, AccountRepository accountRepository) {
        this.policyRepository = policyRepository;
        this.accountRepository = accountRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePolicies() {
        log.info("Запуск проверки просроченных полисов...");

        Instant now = Instant.now();
        List<InsurancePolicy> expiredPolicies = policyRepository.findByStatusAndExpiresAtBefore(PolicyStatus.PENDING, now);

        if (expiredPolicies.isEmpty()) {
            log.info("Просроченных полисов не найдено.");
            return;
        }

        for (InsurancePolicy policy : expiredPolicies) {
            // Блокируем счет, чтобы не пересечься с confirmPolicy
            Account account = accountRepository.findByIdForUpdate(policy.getAccount().getId())
                    .orElseThrow(() -> new RuntimeException("Счет не найден"));

            // Снимаем холд
            account.setHeldAmount(account.getHeldAmount().subtract(policy.getAmount()));
            accountRepository.save(account);

            // Меняем статус
            policy.setStatus(PolicyStatus.EXPIRED);
            policy.setCompletedAt(now);
            policyRepository.save(policy);

            log.info("Полис {} истек. Холд на сумму {} снят.", policy.getId(), policy.getAmount());
        }
    }
}
