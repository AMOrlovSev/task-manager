package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Частичное обновление задачи")
public class TaskUpdateDTO {

    @Schema(description = "Заголовок задачи", example = "Написать отчёт", maxLength = 255)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Schema(
            description = "Описание задачи",
            example = "Подготовить квартальный отчёт",
            maxLength = 4000)
    @Size(max = 4000, message = "Content must be at most 4000 characters")
    private String content;

    @Schema(description = "Порядковый номер", example = "1")
    private Integer index;

    @ArraySchema(
            arraySchema = @Schema(description = "Массив id меток задачи", example = "[1, 2]"),
            schema = @Schema(implementation = Long.class, description = "id метки"))
    @Size(max = 100)
    private List<@NotNull Long> taskLabelIds;

    @Schema(description = "Slug статуса задачи", example = "draft")
    @NotBlank(message = "Status is required")
    private String status;

    @Schema(description = "Идентификатор исполнителя", example = "1")
    @JsonProperty("assignee_id")
    private Long assigneeId;
}
