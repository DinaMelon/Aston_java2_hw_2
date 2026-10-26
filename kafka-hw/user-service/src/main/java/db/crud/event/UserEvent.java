package db.crud.event;

/**
 * Событие, которое user-service отправляет в Kafka.
 * JSON-вид: {"operation":"CREATED","email":"ivan@test.ru"}
 */
public record UserEvent(
        UserOperation operation,
        String email
) {
}
