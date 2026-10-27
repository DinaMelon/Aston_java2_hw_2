package db.crud.event;

/**
 * Тип операции над пользователем, о которой уведомляется notification-service.
 */
public enum UserOperation {
    CREATED,
    DELETED
}
