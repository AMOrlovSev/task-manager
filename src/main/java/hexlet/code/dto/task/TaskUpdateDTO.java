package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskUpdateDTO {

    @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must be at most 255 characters") private String title;

    @Size(max = 4000, message = "Content must be at most 4000 characters") private String content;

    private Integer index;

    @Size(max = 100) private List<@NotNull Long> taskLabelIds;

    @NotBlank(message = "Status is required") private String status;

    @JsonProperty("assignee_id")
    private Long assigneeId;
}
