package ru.amigo.axioma.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.amigo.axioma.notification.dto.PolicyIssuedEvent;
import ru.amigo.axioma.notification.dto.TransferCompletedEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 16:25
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public NotificationListener(ObjectMapper objectMapper, EmailService emailService) {
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    @KafkaListener(topics = "axioma-events", groupId = "notification-group")
    public void handleEvent(String envelopeJson) {
        try {
            log.info("Notification: получено событие из Kafka");

            JsonNode envelope = objectMapper.readTree(envelopeJson);
            String eventType = envelope.get("eventType").asString();
            String payloadJson = envelope.get("payload").asString();

            if ("PolicyIssued".equals(eventType)) {
                PolicyIssuedEvent event = objectMapper.readValue(payloadJson, PolicyIssuedEvent.class);
                emailService.sendPolicyEmail(event.email(), event.policyNumber(), event.amount().toString());
                log.info("Notification: Обработка PolicyIssued. Номер полиса: {}, Email: {}", event.policyNumber(), event.email());
            } else if ("TransferCompleted".equals(eventType)) {
                TransferCompletedEvent event = objectMapper.readValue(payloadJson, TransferCompletedEvent.class);
                log.info("Notification: Обработка TransferCompleted. Сумма: {} {}", event.amount(), event.currency());
                // Имитация отправки SMS
                log.info("SMS-уведомление: Зачислено {} {}", event.amount(), event.currency());
            } else {
                log.warn("Notification: Неизвестный тип события: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Notification: Ошибка при обработке события", e);
        }
    }
}
