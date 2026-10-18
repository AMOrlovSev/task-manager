package hexlet.code.dto.task_status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Частичное обновление статуса задачи")
public class TaskStatusUpdateDTO {

    @Schema(description = "Отображаемое имя статуса", example = "В работе", maxLength = 255)
    @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be at most 255 characters") private String name;

    @Schema(description = "Slug статуса", example = "in_progress", maxLength = 255)
    @NotBlank(message = "Slug is required") @Size(max = 255, message = "Slug must be at most 255 characters") private String slug;
}
