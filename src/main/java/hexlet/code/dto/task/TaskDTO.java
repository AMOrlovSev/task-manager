package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Задача")
public record TaskDTO(
        @Schema(description = "Идентификатор задачи", example = "1") Long id,
        @Schema(description = "Порядковый номер", example = "1") Integer index,
        @Schema(description = "Дата создания", example = "2024-01-15") LocalDate createdAt,
        @Schema(description = "Идентификатор исполнителя", example = "1")
                @JsonProperty("assignee_id")
                Long assigneeId,
        @Schema(description = "Заголовок задачи", example = "Написать отчёт") String title,
        @Schema(description = "Описание задачи", example = "Подготовить квартальный отчёт")
                String content,
        @Schema(description = "Slug статуса", example = "draft") String status,
        @ArraySchema(
                        arraySchema =
                                @Schema(description = "Массив id меток задачи", example = "[1, 2]"),
                        schema = @Schema(implementation = Long.class, description = "id метки"))
                List<Long> taskLabelIds) {}
