package db.crud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank(message = "Имя не должно быть пустым")
        @Size(max = 255, message = "Имя не должно быть длиннее 255 символов")
        String name,

        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не должен быть длиннее 255 символов")
        String email,

        @Min(value = 0, message = "Возраст не может быть отрицательным")
        @Max(value = 150, message = "Возраст не может быть больше 150")
        Integer age
) {
}
