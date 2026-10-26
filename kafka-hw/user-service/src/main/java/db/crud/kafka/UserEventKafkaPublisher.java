package db.crud.kafka;

import db.crud.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Отправляет события о пользователях в Kafka.
 * Сообщение уходит только ПОСЛЕ успешного коммита транзакции,
 * поэтому в Kafka не попадёт событие об операции, которая была откатена.
 */
@Component
public class UserEventKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public UserEventKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserEvent(UserEvent event) {
        try {
            // ключ = email, чтобы события одного пользователя шли в одну партицию по порядку
            kafkaTemplate.send(topic, event.email(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Не удалось отправить событие {} в топик {}", event, topic, ex);
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
        }
    }
}
