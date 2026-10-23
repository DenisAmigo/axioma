package ru.amigo.axioma.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 23:35
 */
@Service
public class MetricsService {

    private final Counter transfersSuccess;
    private final Counter transfersFailed;
    private final Counter policiesIssued;
    private final AtomicReference<BigDecimal> totalHeldAmount = new AtomicReference<>(BigDecimal.ZERO);
    public MetricsService(MeterRegistry registry) {
        // Счетчики (Counter)
        this.transfersSuccess = Counter.builder("axioma.transfers.success")
                .description("Количество успешных переводов")
                .register(registry);

        this.transfersFailed = Counter.builder("axioma.transfers.failed")
                .description("Количество неудачных переводов")
                .register(registry);

        this.policiesIssued = Counter.builder("axioma.policies.issued")
                .description("Количество выпущенных полисов ОСАГО")
                .register(registry);

        // Датчик (Gauge) для отслеживания общей суммы холдов
        Gauge.builder("axioma.holds.total_amount", totalHeldAmount, ref -> ref.get().doubleValue())
                .description("Общая сумма замороженных средств")
                .register(registry);
    }

    public void recordTransferSuccess() {
        transfersSuccess.increment();
    }

    public void recordTransferFailed() {
        transfersFailed.increment();
    }

    public void recordPolicyIssued() {
        policiesIssued.increment();
    }

    public void updateTotalHeldAmount(BigDecimal newAmount) {
        totalHeldAmount.set(newAmount);
    }
}
