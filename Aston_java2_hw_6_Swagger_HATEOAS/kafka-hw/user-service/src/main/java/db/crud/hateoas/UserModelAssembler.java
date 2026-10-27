package db.crud.hateoas;

import db.crud.controller.UserController;
import db.crud.dto.UserResponse;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Превращает {@link UserResponse} в {@link UserModel} и добавляет ссылки:
 * <ul>
 *     <li>{@code self} — сам пользователь ({@code GET /api/users/{id}});</li>
 *     <li>{@code users} — коллекция пользователей ({@code GET /api/users});</li>
 *     <li>{@code update} — обновление ({@code PUT} на тот же URI);</li>
 *     <li>{@code delete} — удаление ({@code DELETE} на тот же URI).</li>
 * </ul>
 */
@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserResponse, UserModel> {

    @Override
    public UserModel toModel(UserResponse user) {
        UserModel model = new UserModel(
                user.id(), user.name(), user.email(), user.age(), user.createdAt());

        Link self = linkTo(UserController.class).slash(user.id()).withSelfRel();
        model.add(self);
        model.add(linkTo(UserController.class).withRel("users"));
        // HTTP-метод в HAL не кодируется: у rel "update"/"delete" тот же URI, метод описан в Swagger
        model.add(self.withRel("update"));
        model.add(self.withRel("delete"));
        return model;
    }
}
