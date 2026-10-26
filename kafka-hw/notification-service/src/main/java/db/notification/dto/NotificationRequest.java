package db.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Тело запроса POST /api/notifications.
 */
public record NotificationRequest(

        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Некорректный формат email")
        String email,

        @NotNull(message = "Операция обязательна (CREATED или DELETED)")
        UserOperation operation
) {
}
