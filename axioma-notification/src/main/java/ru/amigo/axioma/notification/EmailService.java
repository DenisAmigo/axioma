package ru.amigo.axioma.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @author denis.chernonozhkin
 * @since 23.10.2026 18:48
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPolicyEmail(String toEmail, String policyNumber, String amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Ваш полис ОСАГО выпущен");
            message.setText(
                    "Здравствуйте!\n\n" +
                    "Ваш полис ОСАГО успешно оформлен.\n" +
                    "Номер полиса: " + policyNumber + "\n" +
                    "Сумма: " + amount + " RUB\n\n" +
                    "Спасибо, что выбрали Axioma!"
            );

            mailSender.send(message);
            log.info("Email: письмо успешно отправлено на {}", toEmail);
        } catch (Exception e) {
            log.error("Email: ошибка отправки письма на {}", toEmail, e);
        }
    }
}
