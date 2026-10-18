package hexlet.code.dto.task_status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для создания статуса задачи")
public record TaskStatusCreateDTO(
        @Schema(description = "Отображаемое имя статуса", example = "Черновик", maxLength = 255)
                @NotBlank(message = "Name is required")
                @Size(max = 255, message = "Name must be at most 255 characters")
                String name,
        @Schema(
                        description = "Slug статуса (используется в поле status у задачи)",
                        example = "draft",
                        maxLength = 255)
                @NotBlank(message = "Slug is required")
                @Size(max = 255, message = "Slug must be at most 255 characters")
                String slug) {}
