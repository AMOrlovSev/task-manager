package hexlet.code.dto.label;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Метка задачи")
public record LabelDTO(
        @Schema(description = "Идентификатор метки", example = "1") Long id,
        @Schema(description = "Название метки", example = "bug") String name,
        @Schema(description = "Дата создания", example = "2024-01-15") LocalDate createdAt) {}
