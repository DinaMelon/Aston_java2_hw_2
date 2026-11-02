package db.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Что именно было отправлено на почту.
 */
@Schema(description = "Что было отправлено на почту")
public record NotificationResponse(
        String email,
        UserOperation operation,
        String subject,
        String message
) {
}
