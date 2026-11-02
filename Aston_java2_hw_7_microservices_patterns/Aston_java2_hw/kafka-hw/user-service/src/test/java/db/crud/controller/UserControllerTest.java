package db.crud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import db.crud.dto.UserRequest;
import db.crud.dto.UserResponse;
import db.crud.exception.EmailAlreadyExistsException;
import db.crud.exception.UserNotFoundException;
import db.crud.hateoas.UserModelAssembler;
import db.crud.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(UserModelAssembler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static UserResponse response(long id, String name, String email, Integer age) {
        return new UserResponse(id, name, email, age, LocalDateTime.of(2026, 1, 1, 12, 0));
    }

    @Test
    void getAll_shouldReturnHalCollectionOfUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                response(1, "Иван", "ivan@test.ru", 20),
                response(2, "Анна", "anna@test.ru", 22)
        ));

        mockMvc.perform(get("/api/users").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.users.length()").value(2))
                .andExpect(jsonPath("$._embedded.users[0].id").value(1))
                .andExpect(jsonPath("$._embedded.users[0].name").value("Иван"))
                .andExpect(jsonPath("$._embedded.users[1].email").value("anna@test.ru"))
                // ссылки на коллекцию и на каждого пользователя
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/users"))
                .andExpect(jsonPath("$._embedded.users[0]._links.self.href").value("http://localhost/api/users/1"))
                .andExpect(jsonPath("$._embedded.users[1]._links.self.href").value("http://localhost/api/users/2"));
    }

    @Test
    void getAll_shouldReturnCollectionLinkWhenNoUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/users"));
    }

    @Test
    void getById_shouldReturnUserWithHateoasLinks() throws Exception {
        when(userService.getUser(1L)).thenReturn(response(1, "Иван", "ivan@test.ru", 20));

        mockMvc.perform(get("/api/users/1").accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@test.ru"))
                .andExpect(jsonPath("$.age").value(20))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/users/1"))
                .andExpect(jsonPath("$._links.users.href").value("http://localhost/api/users"))
                .andExpect(jsonPath("$._links.update.href").value("http://localhost/api/users/1"))
                .andExpect(jsonPath("$._links.delete.href").value("http://localhost/api/users/1"));
    }

    @Test
    void getById_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUser(999L)).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с id=999 не найден"));
    }

    @Test
    void getById_shouldReturn400WhenIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/api/users/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn201WithLocationAndBody() throws Exception {
        UserRequest request = new UserRequest("Иван", "ivan@test.ru", 20);
        when(userService.createUser(any(UserRequest.class)))
                .thenReturn(response(1, "Иван", "ivan@test.ru", 20));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@test.ru"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/users/1"));
    }

    @Test
    void create_shouldReturn400WhenValidationFails() throws Exception {
        UserRequest invalid = new UserRequest("", "not-an-email", -5);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.age").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    void create_shouldReturn400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not a json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        UserRequest request = new UserRequest("Иван", "ivan@test.ru", 20);
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("ivan@test.ru"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void update_shouldReturnUpdatedUser() throws Exception {
        UserRequest request = new UserRequest("Пётр", "petr@test.ru", 30);
        when(userService.updateUser(eq(1L), any(UserRequest.class)))
                .thenReturn(response(1, "Пётр", "petr@test.ru", 30));

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Пётр"))
                .andExpect(jsonPath("$.email").value("petr@test.ru"))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/users/1"));
    }

    @Test
    void update_shouldReturn404WhenUserNotFound() throws Exception {
        UserRequest request = new UserRequest("Пётр", "petr@test.ru", 30);
        when(userService.updateUser(eq(999L), any(UserRequest.class)))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn400WhenValidationFails() throws Exception {
        UserRequest invalid = new UserRequest(" ", "ivan@test.ru", 200);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.age").exists());
    }

    @Test
    void update_shouldReturn409WhenEmailTakenByAnotherUser() throws Exception {
        UserRequest request = new UserRequest("Пётр", "taken@test.ru", 30);
        when(userService.updateUser(eq(1L), any(UserRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("taken@test.ru"));

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void delete_shouldReturn404WhenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException(999L)).when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound());
    }
}
