package db.notification.integration;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты отправки писем.
 * <p>
 * Kafka — встроенный брокер (@EmbeddedKafka), SMTP — GreenMail (настоящий SMTP-сервер в JVM).
 * Письма реально уходят через JavaMailSender по протоколу SMTP и читаются из «почтового ящика» GreenMail.
 * Docker не нужен.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = NotificationIntegrationTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationIntegrationTest {

    static final String TOPIC = "user-events";

    private static final GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

    static {
        greenMail.start();
    }

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
    }

    @AfterAll
    static void stopMailServer() {
        greenMail.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    // ------------------------------------------------------------------
    // Kafka -> письмо
    // ------------------------------------------------------------------

    @Test
    void kafkaCreatedEvent_shouldSendWelcomeEmail() throws Exception {
        sendToKafka("kafka-created@test.ru",
                "{\"operation\":\"CREATED\",\"email\":\"kafka-created@test.ru\"}");

        MimeMessage mail = awaitMailTo("kafka-created@test.ru");

        assertEquals("Аккаунт создан", mail.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.", bodyOf(mail));
    }

    @Test
    void kafkaDeletedEvent_shouldSendGoodbyeEmail() throws Exception {
        sendToKafka("kafka-deleted@test.ru",
                "{\"operation\":\"DELETED\",\"email\":\"kafka-deleted@test.ru\"}");

        MimeMessage mail = awaitMailTo("kafka-deleted@test.ru");

        assertEquals("Аккаунт удалён", mail.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был удалён.", bodyOf(mail));
    }

    @Test
    void kafkaMalformedMessage_shouldBeSkippedAndNextMessageProcessed() throws Exception {
        // «битое» сообщение не должно ронять consumer или блокировать очередь
        sendToKafka("broken", "это вообще не JSON");
        sendToKafka("after-broken@test.ru",
                "{\"operation\":\"CREATED\",\"email\":\"after-broken@test.ru\"}");

        MimeMessage mail = awaitMailTo("after-broken@test.ru");

        assertEquals("Аккаунт создан", mail.getSubject());
    }

    // ------------------------------------------------------------------
    // REST API -> письмо
    // ------------------------------------------------------------------

    @Test
    void api_shouldSendCreatedEmail() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-created@test.ru\",\"operation\":\"CREATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("api-created@test.ru"))
                .andExpect(jsonPath("$.operation").value("CREATED"));

        MimeMessage mail = awaitMailTo("api-created@test.ru");

        assertEquals("Аккаунт создан", mail.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.", bodyOf(mail));
    }

    @Test
    void api_shouldSendDeletedEmail() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-deleted@test.ru\",\"operation\":\"DELETED\"}"))
                .andExpect(status().isOk());

        MimeMessage mail = awaitMailTo("api-deleted@test.ru");

        assertEquals("Аккаунт удалён", mail.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был удалён.", bodyOf(mail));
    }

    @Test
    void api_shouldReturn400AndSendNothingForInvalidRequest() throws Exception {
        int before = greenMail.getReceivedMessages().length;

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"operation\":\"CREATED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        assertEquals(before, greenMail.getReceivedMessages().length);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void sendToKafka(String key, String json) throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TOPIC, key, json)).get();
        }
    }

    /**
     * Ждёт (до 20 секунд), пока в почтовом сервере появится письмо для указанного адресата.
     */
    private MimeMessage awaitMailTo(String email) throws Exception {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            for (MimeMessage message : greenMail.getReceivedMessages()) {
                Address[] recipients = message.getRecipients(Message.RecipientType.TO);
                if (recipients != null && recipients.length == 1
                        && email.equals(((InternetAddress) recipients[0]).getAddress())) {
                    assertNotNull(message.getFrom());
                    assertTrue(message.getFrom().length > 0);
                    return message;
                }
            }
            Thread.sleep(100);
        }
        return fail("Письмо для " + email + " не пришло за 20 секунд");
    }

    private static String bodyOf(MimeMessage message) throws Exception {
        return ((String) message.getContent()).trim();
    }
}
