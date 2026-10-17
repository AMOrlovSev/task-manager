package hexlet.code.dto.task_status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateDTO {

    @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be at most 255 characters") private String name;

    @NotBlank(message = "Slug is required") @Size(max = 255, message = "Slug must be at most 255 characters") private String slug;
}
