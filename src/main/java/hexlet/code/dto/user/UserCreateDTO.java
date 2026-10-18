package hexlet.code.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для регистрации пользователя")
public record UserCreateDTO(
        @Schema(description = "Имя", example = "Иван") String firstName,
        @Schema(description = "Фамилия", example = "Иванов") String lastName,
        @Schema(description = "Email", example = "hexlet@example.com", maxLength = 255)
                @NotBlank(message = "Email is required") @Size(max = 255, message = "Email must be at most 255 characters") @Email(message = "Invalid email format") String email,
        @Schema(description = "Пароль", example = "qwerty", minLength = 3)
                @NotBlank(message = "Password is required") @Size(min = 3, message = "Password must be at least 3 characters") String password) {}
