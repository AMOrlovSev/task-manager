package hexlet.code.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Частичное обновление пользователя")
public class UserUpdateDTO {

    @Schema(description = "Email", example = "hexlet@example.com", maxLength = 255)
    @NotBlank(message = "Email is required") @Size(max = 255, message = "Email must be at most 255 characters") @Email(message = "Invalid email format") private String email;

    @Schema(description = "Новый пароль", example = "qwerty", minLength = 3)
    @Size(min = 3, message = "Password must be at least 3 characters") private String password;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов")
    private String lastName;
}
