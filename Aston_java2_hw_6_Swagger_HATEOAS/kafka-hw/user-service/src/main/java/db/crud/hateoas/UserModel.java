package db.crud.hateoas;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

/**
 * Представление пользователя с HATEOAS-ссылками (блок {@code _links} в HAL).
 */
@Relation(itemRelation = "user", collectionRelation = "users")
@Schema(name = "User", description = "Пользователь + ссылки для навигации (HAL)")
public class UserModel extends RepresentationModel<UserModel> {

    @Schema(description = "Идентификатор", example = "1")
    private final Long id;

    @Schema(description = "Имя", example = "Иван")
    private final String name;

    @Schema(description = "Email (уникальный)", example = "ivan@test.ru")
    private final String email;

    @Schema(description = "Возраст", example = "20")
    private final Integer age;

    @Schema(description = "Дата и время создания", example = "2026-01-01T12:00:00")
    private final LocalDateTime createdAt;

    public UserModel(Long id, String name, String email, Integer age, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
