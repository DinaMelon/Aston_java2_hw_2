package db.notification.controller;

import db.notification.dto.ErrorResponse;
import db.notification.dto.NotificationRequest;
import db.notification.dto.NotificationResponse;
import db.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-вариант того же функционала, что и Kafka: отправить письмо о создании/удалении аккаунта.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Ручная отправка e-mail уведомлений")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Отправить письмо о создании/удалении аккаунта",
            description = "`operation`: `CREATED` — письмо о создании аккаунта, `DELETED` — об удалении.")
    @ApiResponse(responseCode = "200", description = "Письмо отправлено; в теле — тема и текст письма")
    @ApiResponse(responseCode = "400", description = "Невалидный запрос",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "Почтовый сервер не принял письмо",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        return notificationService.notifyUser(request.operation(), request.email());
    }
}
