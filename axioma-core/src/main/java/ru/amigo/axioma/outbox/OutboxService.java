package ru.amigo.axioma.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 1:03
 */
@Service
public class OutboxService {

    private static Logger log = LoggerFactory.getLogger(OutboxService.class);
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void saveEvent(String aggregateType, Long aggregateId, String eventType, Object payloadObject) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payloadObject);
            OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, payloadJson);
            outboxRepository.save(event);
            log.info("Outbox: сохранено событие {} для {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Outbox: ошибка сериализации события {}", eventType, e);
            throw new RuntimeException("Не удалось сериализовать событие", e);
        }
    }
}
