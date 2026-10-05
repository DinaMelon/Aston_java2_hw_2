package db.crud.service;

import db.crud.dao.UserDaoInterface;
import db.crud.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserDaoInterface userDao;

    @InjectMocks
    private UserService userService;
    @Test
    void createUser_shouldCreateUser() {

        userService.createUser(
                "Иван",
                "ivan@test.ru",
                20
        );

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userDao).create(captor.capture());

        User user = captor.getValue();

        assertEquals("Иван", user.getName());
        assertEquals("ivan@test.ru", user.getEmail());
        assertEquals(20, user.getAge());
    }

    @Test
    void getUser_shouldReturnUser() {

        User user = new User(
                "Иван",
                "ivan@test.ru",
                20
        );

        when(userDao.read(1L))
                .thenReturn(user);

        User result = userService.getUser(1L);

        assertNotNull(result);

        assertEquals("Иван", result.getName());
        assertEquals("ivan@test.ru", result.getEmail());
        assertEquals(20, result.getAge());

        verify(userDao).read(1L);
    }
    @Test
    void getUser_shouldReturnNullWhenUserDoesNotExist() {

        when(userDao.read(999L))
                .thenReturn(null);

        User result = userService.getUser(999L);

        assertNull(result);

        verify(userDao).read(999L);
    }
    @Test
    void getAllUsers_shouldReturnAllUsers() {

        User user1 = new User(
                "Иван",
                "ivan@test.ru",
                20
        );

        User user2 = new User(
                "Анна",
                "anna@test.ru",
                22
        );

        List<User> users = List.of(
                user1,
                user2
        );

        when(userDao.readAll())
                .thenReturn(users);

        List<User> result =
                userService.getAllUsers();

        assertNotNull(result);

        assertEquals(2, result.size());

        assertEquals(
                "Иван",
                result.get(0).getName()
        );

        assertEquals(
                "Анна",
                result.get(1).getName()
        );

        verify(userDao).readAll();
    }
    @Test
    void getAllUsers_shouldReturnEmptyList() {

        when(userDao.readAll())
                .thenReturn(List.of());

        List<User> result =
                userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userDao).readAll();
    }
    @Test
    void updateUser_shouldUpdateExistingUser() {

        User user = new User(
                "Иван",
                "old@test.ru",
                20
        );

        when(userDao.read(1L))
                .thenReturn(user);

        userService.updateUser(
                1L,
                "Петр",
                "new@test.ru",
                25
        );

        assertEquals(
                "Петр",
                user.getName()
        );

        assertEquals(
                "new@test.ru",
                user.getEmail()
        );

        assertEquals(
                25,
                user.getAge()
        );

        verify(userDao).read(1L);
        verify(userDao).update(user);
    }

    @Test
    void updateUser_shouldNotUpdateWhenUserDoesNotExist() {

        when(userDao.read(999L))
                .thenReturn(null);

        userService.updateUser(
                999L,
                "Петр",
                "petr@test.ru",
                25
        );

        verify(userDao).read(999L);

        verify(
                userDao,
                never()
        ).update(any(User.class));
    }
    @Test
    void deleteUser_shouldDeleteUser() {

        userService.deleteUser(1L);

        verify(userDao).delete(1L);
    }
}