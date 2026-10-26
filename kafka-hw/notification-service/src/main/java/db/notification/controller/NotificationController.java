package db.notification.controller;

import db.notification.dto.NotificationRequest;
import db.notification.dto.NotificationResponse;
import db.notification.service.NotificationService;
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
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        return notificationService.notifyUser(request.operation(), request.email());
    }
}
