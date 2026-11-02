package db.notification.service;

import db.notification.dto.NotificationResponse;
import db.notification.dto.UserOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(emailService, "ваш сайт");
    }

    @Test
    void notifyUser_shouldSendCreatedMessage() {
        NotificationResponse response = notificationService.notifyUser(UserOperation.CREATED, "ivan@test.ru");

        String expected = "Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.";
        verify(emailService).send("ivan@test.ru", "Аккаунт создан", expected);

        assertEquals("ivan@test.ru", response.email());
        assertEquals(UserOperation.CREATED, response.operation());
        assertEquals(expected, response.message());
    }

    @Test
    void notifyUser_shouldSendDeletedMessage() {
        NotificationResponse response = notificationService.notifyUser(UserOperation.DELETED, "ivan@test.ru");

        String expected = "Здравствуйте! Ваш аккаунт был удалён.";
        verify(emailService).send("ivan@test.ru", "Аккаунт удалён", expected);

        assertEquals(UserOperation.DELETED, response.operation());
        assertEquals(expected, response.message());
    }

    @Test
    void notifyUser_shouldUseConfiguredSiteName() {
        NotificationService service = new NotificationService(emailService, "MyShop");

        service.notifyUser(UserOperation.CREATED, "ivan@test.ru");

        verify(emailService).send("ivan@test.ru", "Аккаунт создан",
                "Здравствуйте! Ваш аккаунт на сайте MyShop был успешно создан.");
    }

    @Test
    void notifyUser_shouldPropagateMailErrors() {
        doThrow(new MailSendException("smtp down"))
                .when(emailService).send("ivan@test.ru", "Аккаунт удалён", "Здравствуйте! Ваш аккаунт был удалён.");

        assertThrows(MailSendException.class,
                () -> notificationService.notifyUser(UserOperation.DELETED, "ivan@test.ru"));
    }
}
