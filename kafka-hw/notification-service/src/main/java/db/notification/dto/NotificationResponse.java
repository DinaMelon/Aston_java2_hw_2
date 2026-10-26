package db.notification.dto;

/**
 * Что именно было отправлено на почту.
 */
public record NotificationResponse(
        String email,
        UserOperation operation,
        String subject,
        String message
) {
}
