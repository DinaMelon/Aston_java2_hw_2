package db.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Тело запроса POST /api/notifications.
 */
@Schema(description = "Запрос на отправку уведомления")
public record NotificationRequest(

        @Schema(description = "Адрес получателя", example = "ivan@test.ru")
        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Некорректный формат email")
        String email,

        @Schema(description = "Тип операции", example = "CREATED")
        @NotNull(message = "Операция обязательна (CREATED или DELETED)")
        UserOperation operation
) {
}
