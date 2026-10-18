package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Данные для создания задачи")
public record TaskCreateDTO(
        @Schema(description = "Заголовок задачи", example = "Написать отчёт", maxLength = 255)
                @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must be at most 255 characters") String title,
        @Schema(
                        description = "Описание задачи",
                        example = "Подготовить квартальный отчёт",
                        maxLength = 4000)
                @Size(max = 4000, message = "Content must be at most 4000 characters") String content,
        @Schema(description = "Порядковый номер задачи", example = "1") Integer index,
        @Schema(description = "Slug статуса задачи", example = "draft")
                @NotBlank(message = "Status is required") String status,
        @Schema(description = "Идентификатор исполнителя", example = "1")
                @JsonProperty("assignee_id")
                Long assigneeId,
        @ArraySchema(
                        arraySchema =
                                @Schema(description = "Массив id меток задачи", example = "[1, 2]"),
                        schema = @Schema(implementation = Long.class, description = "id метки"))
                @Size(max = 100) List<@NotNull Long> taskLabelIds) {}
