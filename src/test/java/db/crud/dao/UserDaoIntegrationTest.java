package db.crud.dao;

import db.crud.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDaoIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withFixedExposedPort(5433, 5432);


    private UserDaoImpl userDao;

    @BeforeAll
    void setUp() throws Exception {

        waitForDatabase();

        userDao = new UserDaoImpl();
    }

    @BeforeEach
    void cleanDatabase() throws Exception {

        try (
                Connection connection =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword()
                        );

                Statement statement =
                        connection.createStatement()
        ) {

            statement.executeUpdate(
                    "DELETE FROM users"
            );
        }
    }
    @AfterAll
    void tearDown() {
        // Контейнер останавливается автоматически
        // благодаря @Container.
    }

    @Test
    void create_shouldSaveUser() {

        User user = new User(
                "Иван",
                "ivan@test.ru",
                20
        );

        userDao.create(user);

        assertNotNull(user.getId());

        User result =
                userDao.read(user.getId());

        assertNotNull(result);

        assertEquals(
                "Иван",
                result.getName()
        );

        assertEquals(
                "ivan@test.ru",
                result.getEmail()
        );

        assertEquals(
                20,
                result.getAge()
        );
    }

    @Test
    void read_shouldReturnUser() {

        User user = new User(
                "Анна",
                "anna@test.ru",
                22
        );

        userDao.create(user);

        User result =
                userDao.read(user.getId());

        assertNotNull(result);

        assertEquals(
                user.getId(),
                result.getId()
        );

        assertEquals(
                "Анна",
                result.getName()
        );

        assertEquals(
                "anna@test.ru",
                result.getEmail()
        );

        assertEquals(
                22,
                result.getAge()
        );
    }

    @Test
    void read_shouldReturnNullForNonExistingUser() {

        User result =
                userDao.read(999999L);

        assertNull(result);
    }
    @Test
    void readAll_shouldReturnAllUsers() {

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

        userDao.create(user1);
        userDao.create(user2);

        List<User> users =
                userDao.readAll();

        assertEquals(
                2,
                users.size()
        );
    }
    @Test
    void update_shouldUpdateUser() {

        User user = new User(
                "Иван",
                "ivan@test.ru",
                20
        );

        userDao.create(user);

        user.setName("Петр");
        user.setEmail("petr@test.ru");
        user.setAge(25);

        userDao.update(user);

        User result =
                userDao.read(user.getId());

        assertNotNull(result);

        assertEquals(
                "Петр",
                result.getName()
        );

        assertEquals(
                "petr@test.ru",
                result.getEmail()
        );

        assertEquals(
                25,
                result.getAge()
        );
    }
    @Test
    void delete_shouldDeleteUser() {

        User user = new User(
                "Иван",
                "ivan@test.ru",
                20
        );

        userDao.create(user);

        long id = user.getId();

        userDao.delete(id);

        User result =
                userDao.read(id);

        assertNull(result);
    }
    @Test
    void delete_shouldDoNothingForNonExistingUser() {

        assertDoesNotThrow(
                () -> userDao.delete(999999L)
        );

        assertNull(
                userDao.read(999999L)
        );
    }
    private void waitForDatabase()
            throws InterruptedException {

        int attempts = 30;

        while (attempts > 0) {

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    postgres.getJdbcUrl(),
                                    postgres.getUsername(),
                                    postgres.getPassword()
                            )
            ) {

                return;

            } catch (Exception e) {

                attempts--;

                Thread.sleep(500);
            }
        }

        throw new IllegalStateException(
                "Не удалось подключиться к PostgreSQL Testcontainer"
        );
    }
}