package db.crud.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.crud.dto.UserRequest;
import db.crud.repository.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@EmbeddedKafka(
        partitions = 1,
        topics = UserApiIntegrationTest.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class UserApiIntegrationTest {

    static final String TOPIC = "user-events";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    private long createUser(String name, String email, Integer age) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(name, email, age))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void fullCrudFlow() throws Exception {
        // create
        long id = createUser("Иван", "ivan@test.ru", 20);

        // read one
        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@test.ru"))
                .andExpect(jsonPath("$.age").value(20))
                .andExpect(jsonPath("$.createdAt").exists());

        // read all
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // update
        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("Пётр", "petr@test.ru", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Пётр"))
                .andExpect(jsonPath("$.email").value("petr@test.ru"))
                .andExpect(jsonPath("$.age").value(30));

        assertEquals("Пётр", userRepository.findById(id).orElseThrow().getName());

        // delete
        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isNotFound());

        assertEquals(0, userRepository.count());
    }

    @Test
    void create_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        createUser("Иван", "ivan@test.ru", 20);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("Другой", "ivan@test.ru", 40))))
                .andExpect(status().isConflict());

        assertEquals(1, userRepository.count());
    }

    @Test
    void update_shouldReturn409WhenEmailBelongsToAnotherUser() throws Exception {
        createUser("Иван", "ivan@test.ru", 20);
        long annaId = createUser("Анна", "anna@test.ru", 22);

        mockMvc.perform(put("/api/users/" + annaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("Анна", "ivan@test.ru", 22))))
                .andExpect(status().isConflict());
    }

    @Test
    void update_shouldAllowKeepingOwnEmail() throws Exception {
        long id = createUser("Иван", "ivan@test.ru", 20);

        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("Иван Иванов", "ivan@test.ru", 21))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Иван Иванов"));
    }

    @Test
    void getById_shouldReturn404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/api/users/12345"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404ForUnknownUser() throws Exception {
        mockMvc.perform(delete("/api/users/12345"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn400ForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("", "bad", -1))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Kafka: проверка событий, которые user-service отправляет в notification-service
    // ------------------------------------------------------------------

    /**
     * Читает топик с самого начала и собирает события для указанного email
     * (ключ сообщения = email). Ждёт, пока наберётся minCount событий, но не дольше timeoutMs.
     */
    private List<JsonNode> collectEvents(String email, int minCount, long timeoutMs) throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        List<JsonNode> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeoutMs;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            while (events.size() < minCount && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(300))) {
                    if (email.equals(record.key())) {
                        events.add(objectMapper.readTree(record.value()));
                    }
                }
            }
        }
        return events;
    }

    @Test
    void createUser_shouldSendCreatedEventToKafka() throws Exception {
        createUser("Иван", "kafka-create@test.ru", 20);

        List<JsonNode> events = collectEvents("kafka-create@test.ru", 1, 15_000);

        assertEquals(1, events.size());
        assertEquals("CREATED", events.get(0).get("operation").asText());
        assertEquals("kafka-create@test.ru", events.get(0).get("email").asText());
    }

    @Test
    void deleteUser_shouldSendDeletedEventToKafka() throws Exception {
        long id = createUser("Иван", "kafka-delete@test.ru", 20);

        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isNoContent());

        List<JsonNode> events = collectEvents("kafka-delete@test.ru", 2, 15_000);

        assertEquals(2, events.size());
        assertEquals("CREATED", events.get(0).get("operation").asText());
        assertEquals("DELETED", events.get(1).get("operation").asText());
        assertEquals("kafka-delete@test.ru", events.get(1).get("email").asText());
    }

    @Test
    void createUser_shouldNotSendEventWhenEmailAlreadyExists() throws Exception {
        createUser("Иван", "kafka-dup@test.ru", 20);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest("Другой", "kafka-dup@test.ru", 40))))
                .andExpect(status().isConflict());

        // ждём второе событие, которого быть не должно -> по таймауту остаётся ровно одно
        List<JsonNode> events = collectEvents("kafka-dup@test.ru", 2, 4_000);

        assertEquals(1, events.size());
    }
}
