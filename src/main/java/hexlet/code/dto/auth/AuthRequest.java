package hexlet.code.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Учётные данные для получения JWT")
public record AuthRequest(
        @Schema(
                        description = "Email пользователя",
                        example = "hexlet@example.com",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String username,
        @Schema(
                        description = "Пароль",
                        example = "qwerty",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String password) {}
