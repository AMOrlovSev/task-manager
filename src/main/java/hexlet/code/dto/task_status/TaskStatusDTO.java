package hexlet.code.dto.task_status;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Статус задачи")
public record TaskStatusDTO(
        @Schema(description = "Идентификатор статуса", example = "1") Long id,
        @Schema(description = "Отображаемое имя статуса", example = "Черновик") String name,
        @Schema(description = "Slug статуса", example = "draft") String slug,
        @Schema(description = "Дата создания", example = "2024-01-15") LocalDate createdAt) {}
