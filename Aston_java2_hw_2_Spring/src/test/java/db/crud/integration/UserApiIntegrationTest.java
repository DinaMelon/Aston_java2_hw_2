package db.crud.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import db.crud.dto.UserRequest;
import db.crud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class UserApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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
}
