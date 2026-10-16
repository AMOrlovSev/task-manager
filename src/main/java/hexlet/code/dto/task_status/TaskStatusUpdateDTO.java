package hexlet.code.dto.task_status;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateDTO {

    @NotBlank(message = "Name is required") private String name;

    @NotBlank(message = "Slug is required") private String slug;
}
