package db.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import db.notification.dto.NotificationRequest;
import db.notification.dto.NotificationResponse;
import db.notification.dto.UserOperation;
import db.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void send_shouldReturn200WithSentMessage() throws Exception {
        when(notificationService.notifyUser(UserOperation.DELETED, "ivan@test.ru"))
                .thenReturn(new NotificationResponse("ivan@test.ru", UserOperation.DELETED,
                        "Аккаунт удалён", "Здравствуйте! Ваш аккаунт был удалён."));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NotificationRequest("ivan@test.ru", UserOperation.DELETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ivan@test.ru"))
                .andExpect(jsonPath("$.operation").value("DELETED"))
                .andExpect(jsonPath("$.message").value("Здравствуйте! Ваш аккаунт был удалён."));
    }

    @Test
    void send_shouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.operation").exists());

        verify(notificationService, never()).notifyUser(any(), any());
    }

    @Test
    void send_shouldReturn400WhenOperationIsUnknown() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ivan@test.ru\",\"operation\":\"UPDATED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void send_shouldReturn502WhenMailServerFails() throws Exception {
        when(notificationService.notifyUser(UserOperation.CREATED, "ivan@test.ru"))
                .thenThrow(new MailSendException("smtp down"));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new NotificationRequest("ivan@test.ru", UserOperation.CREATED))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }
}
