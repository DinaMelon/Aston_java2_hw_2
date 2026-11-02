package db.crud.kafka;

import db.crud.client.NotificationClient;
import db.crud.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

/**
 * Отправляет события о пользователях в Kafka.
 * Сообщение уходит только ПОСЛЕ успешного коммита транзакции,
 * поэтому в Kafka не попадёт событие об операции, которая была откатена.
 * <p>
 * Если Kafka недоступна, уведомление отправляется напрямую в notification-service по REST
 * (через Eureka и Circuit Breaker, см. {@link NotificationClient}).
 */
@Component
public class UserEventKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;
    private final NotificationClient notificationClient;

    public UserEventKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${app.kafka.topic}") String topic,
                                   NotificationClient notificationClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.notificationClient = notificationClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserEvent(UserEvent event) {
        try {
            // ключ = email, чтобы события одного пользователя шли в одну партицию по порядку
            kafkaTemplate.send(topic, event.email(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Не удалось отправить событие {} в топик {}", event, topic, ex);
                    sendViaRest(event);
                } else {
                    log.info("Событие {} отправлено в топик {} (partition={}, offset={})",
                            event, topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            // недоступность Kafka не должна ломать CRUD API
            log.error("Ошибка при отправке события {} в Kafka", event, e);
            sendViaRest(event);
        }
    }

    /** Резервный канал; выполняется в отдельном потоке, чтобы не блокировать ни запрос, ни поток Kafka-producer'а. */
    private void sendViaRest(UserEvent event) {
        CompletableFuture.runAsync(() -> notificationClient.send(event));
    }
}
