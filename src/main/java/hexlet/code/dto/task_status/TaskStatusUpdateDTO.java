package hexlet.code.dto.task_status;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskStatusUpdateDTO {

    @Size(min = 1, message = "Name must be at least 1 character") private String name;

    @Size(min = 1, message = "Slug must be at least 1 character") private String slug;
}
