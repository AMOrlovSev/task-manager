package hexlet.code.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Пользователь")
public record UserDTO(
        @Schema(description = "Идентификатор пользователя", example = "1") Long id,
        @Schema(description = "Имя", example = "Иван") String firstName,
        @Schema(description = "Фамилия", example = "Иванов") String lastName,
        @Schema(description = "Email", example = "hexlet@example.com") String email,
        @Schema(description = "Дата создания", example = "2024-01-15") LocalDate createdAt) {}
