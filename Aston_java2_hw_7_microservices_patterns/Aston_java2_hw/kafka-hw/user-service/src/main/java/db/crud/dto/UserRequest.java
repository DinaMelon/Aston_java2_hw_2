package db.crud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Тело запроса для создания/обновления пользователя")
public record UserRequest(

        @Schema(description = "Имя", example = "Иван", maxLength = 255)
        @NotBlank(message = "Имя не должно быть пустым")
        @Size(max = 255, message = "Имя не должно быть длиннее 255 символов")
        String name,

        @Schema(description = "Email (уникальный)", example = "ivan@test.ru", maxLength = 255)
        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не должен быть длиннее 255 символов")
        String email,

        @Schema(description = "Возраст (0–150), необязательное поле", example = "20", minimum = "0", maximum = "150")
        @Min(value = 0, message = "Возраст не может быть отрицательным")
        @Max(value = 150, message = "Возраст не может быть больше 150")
        Integer age
) {
}
