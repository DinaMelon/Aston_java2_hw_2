package db.notification.dto;

/**
 * Сообщение из Kafka (топик user-events).
 * JSON-вид: {"operation":"CREATED","email":"ivan@test.ru"}
 */
public record UserEvent(
        UserOperation operation,
        String email
) {
}
