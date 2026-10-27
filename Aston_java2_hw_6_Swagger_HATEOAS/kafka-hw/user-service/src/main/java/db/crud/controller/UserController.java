package db.crud.controller;

import db.crud.dto.ErrorResponse;
import db.crud.dto.UserRequest;
import db.crud.dto.UserResponse;
import db.crud.hateoas.UserModel;
import db.crud.hateoas.UserModelAssembler;
import db.crud.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Управление пользователями (CRUD). Ответы содержат HATEOAS-ссылки в `_links`")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler assembler;

    public UserController(UserService userService, UserModelAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @Operation(summary = "Получить всех пользователей",
            description = "Возвращает коллекцию в формате HAL: пользователи лежат в `_embedded.users`, "
                    + "ссылка на саму коллекцию — в `_links.self`.")
    @ApiResponse(responseCode = "200", description = "Список пользователей (может быть пустым)")
    @GetMapping
    public CollectionModel<UserModel> getAll() {
        CollectionModel<UserModel> users = assembler.toCollectionModel(userService.getAllUsers());
        users.add(linkTo(UserController.class).withSelfRel());
        return users;
    }

    @Operation(summary = "Получить пользователя по id")
    @ApiResponse(responseCode = "200", description = "Пользователь найден")
    @ApiResponse(responseCode = "400", description = "id не является числом",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public UserModel getById(@Parameter(description = "Идентификатор пользователя", example = "1")
                             @PathVariable("id") Long id) {
        return assembler.toModel(userService.getUser(id));
    }

    @Operation(summary = "Создать пользователя",
            description = "Создаёт пользователя и отправляет событие `CREATED` в Kafka. "
                    + "URL созданного ресурса — в заголовке `Location` и в `_links.self`.")
    @ApiResponse(responseCode = "201", description = "Пользователь создан")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email уже занят",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<UserModel> create(@Valid @RequestBody UserRequest request) {
        UserResponse created = userService.createUser(request);
        UserModel model = assembler.toModel(created);

        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    @Operation(summary = "Обновить пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email принадлежит другому пользователю",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public UserModel update(@Parameter(description = "Идентификатор пользователя", example = "1")
                            @PathVariable("id") Long id,
                            @Valid @RequestBody UserRequest request) {
        return assembler.toModel(userService.updateUser(id, request));
    }

    @Operation(summary = "Удалить пользователя",
            description = "Удаляет пользователя и отправляет событие `DELETED` в Kafka.")
    @ApiResponse(responseCode = "204", description = "Пользователь удалён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Идентификатор пользователя", example = "1")
                       @PathVariable("id") Long id) {
        userService.deleteUser(id);
    }
}
