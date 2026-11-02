package db.notification.kafka;

import db.notification.dto.UserEvent;
import db.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Получает события о создании/удалении пользователя из Kafka и отправляет письмо.
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final NotificationService notificationService;

    public UserEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onUserEvent(@Payload(required = false) UserEvent event) {
        if (event == null || event.operation() == null || !StringUtils.hasText(event.email())) {
            log.warn("Получено некорректное событие, пропускаю: {}", event);
            return;
        }

        log.info("Получено событие из Kafka: {}", event);
        // при ошибке отправки (MailException) сработает DefaultErrorHandler -> повторные попытки
        notificationService.notifyUser(event.operation(), event.email());
    }
}
