package db.notification.service;

import db.notification.dto.NotificationResponse;
import db.notification.dto.UserOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Формирует текст письма в зависимости от операции и отправляет его.
 * Используется и Kafka-listener'ом, и REST-контроллером — логика одна и та же.
 */
@Service
public class NotificationService {

    private final EmailService emailService;
    private final String siteName;

    public NotificationService(EmailService emailService,
                               @Value("${app.site-name:ваш сайт}") String siteName) {
        this.emailService = emailService;
        this.siteName = siteName;
    }

    public NotificationResponse notifyUser(UserOperation operation, String email) {
        String subject = subjectFor(operation);
        String text = textFor(operation);

        emailService.send(email, subject, text);

        return new NotificationResponse(email, operation, subject, text);
    }

    String subjectFor(UserOperation operation) {
        return switch (operation) {
            case CREATED -> "Аккаунт создан";
            case DELETED -> "Аккаунт удалён";
        };
    }

    String textFor(UserOperation operation) {
        return switch (operation) {
            case CREATED -> "Здравствуйте! Ваш аккаунт на сайте " + siteName + " был успешно создан.";
            case DELETED -> "Здравствуйте! Ваш аккаунт был удалён.";
        };
    }
}
