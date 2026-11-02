package db.crud.service;

import db.crud.dto.UserRequest;
import db.crud.dto.UserResponse;
import db.crud.event.UserEvent;
import db.crud.event.UserOperation;
import db.crud.exception.EmailAlreadyExistsException;
import db.crud.exception.UserNotFoundException;
import db.crud.mapper.UserMapper;
import db.crud.model.User;
import db.crud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new UserMapper(), eventPublisher);
    }

    private static User userWithId(long id, String name, String email, Integer age) {
        User user = new User(name, email, age);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void getAllUsers_shouldReturnMappedDtos() {
        when(userRepository.findAll()).thenReturn(List.of(
                userWithId(1L, "Иван", "ivan@test.ru", 20),
                userWithId(2L, "Анна", "anna@test.ru", 22)
        ));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("Иван", result.get(0).name());
        assertEquals("anna@test.ru", result.get(1).email());
    }

    @Test
    void getAllUsers_shouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertTrue(userService.getAllUsers().isEmpty());
    }

    @Test
    void getUser_shouldReturnDto() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(userWithId(1L, "Иван", "ivan@test.ru", 20)));

        UserResponse result = userService.getUser(1L);

        assertEquals(1L, result.id());
        assertEquals("Иван", result.name());
        assertEquals("ivan@test.ru", result.email());
        assertEquals(20, result.age());
    }

    @Test
    void getUser_shouldThrowWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(999L));
    }

    @Test
    void createUser_shouldSaveAndReturnDto() {
        when(userRepository.existsByEmail("ivan@test.ru")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        UserResponse result = userService.createUser(new UserRequest("Иван", "ivan@test.ru", 20));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Иван", captor.getValue().getName());
        assertEquals("ivan@test.ru", captor.getValue().getEmail());
        assertEquals(20, captor.getValue().getAge());

        assertEquals(1L, result.id());
        assertEquals("Иван", result.name());

        verify(eventPublisher).publishEvent(new UserEvent(UserOperation.CREATED, "ivan@test.ru"));
    }

    @Test
    void createUser_shouldThrowWhenEmailExists() {
        when(userRepository.existsByEmail("ivan@test.ru")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> userService.createUser(new UserRequest("Иван", "ivan@test.ru", 20)));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void updateUser_shouldUpdateFields() {
        User existing = userWithId(1L, "Иван", "old@test.ru", 20);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("new@test.ru", 1L)).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);

        UserResponse result = userService.updateUser(1L, new UserRequest("Пётр", "new@test.ru", 25));

        assertEquals("Пётр", existing.getName());
        assertEquals("new@test.ru", existing.getEmail());
        assertEquals(25, existing.getAge());

        assertEquals(1L, result.id());
        assertEquals("Пётр", result.name());
        assertEquals("new@test.ru", result.email());
        assertEquals(25, result.age());
    }

    @Test
    void updateUser_shouldThrowWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateUser(999L, new UserRequest("Пётр", "petr@test.ru", 25)));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldThrowWhenEmailTakenByAnotherUser() {
        User existing = userWithId(1L, "Иван", "ivan@test.ru", 20);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("taken@test.ru", 1L)).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> userService.updateUser(1L, new UserRequest("Иван", "taken@test.ru", 20)));

        assertEquals("ivan@test.ru", existing.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_shouldDeleteExistingUserAndPublishEvent() {
        User existing = userWithId(1L, "Иван", "ivan@test.ru", 20);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.deleteUser(1L);

        verify(userRepository).delete(existing);
        verify(eventPublisher).publishEvent(new UserEvent(UserOperation.DELETED, "ivan@test.ru"));
    }

    @Test
    void deleteUser_shouldThrowWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(999L));

        verify(userRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
