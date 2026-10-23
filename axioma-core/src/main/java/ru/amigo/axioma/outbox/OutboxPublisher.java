package ru.amigo.axioma.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 1:25
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC_NAME = "axioma-events";

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Запускаем каждые 2 секунды после завершения предыдущей итерации
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox: найдено {} событий для отправки в Kafka", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Map<String, Object> envelope = Map.of(
                    "eventType", event.getEventType(),
                    "aggregateType", event.getAggregateType(),
                    "aggregateId", event.getAggregateId(),
                    "payload", event.getPayload()
                );
                String envelopeJson = new ObjectMapper().writeValueAsString(envelope);

                // Отправляем в Kafka. Ключ (event.getAggregateId()) гарантирует,
                // что все события по одному переводу/полису попадут в одну партицию.
                kafkaTemplate.send(TOPIC_NAME, String.valueOf(event.getAggregateId()), envelopeJson)
                        .whenComplete((result, exception) -> {
                            if (exception == null) {
                                log.info("Outbox: событие {} успешно отправлено в Kafka", event.getId());
                            } else {
                                log.error("Outbox: ошибка отправки события {} в Kafka", event.getId(), exception);
                            }
                        });

                // Помечаем как SENT
                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Outbox: критическая ошибка при отправке события {}", event.getId(), e);
                event.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }
}
