package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TaskCreateDTO(
        @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must be at most 255 characters") String title,
        @Size(max = 4000, message = "Content must be at most 4000 characters") String content,
        Integer index,
        @NotBlank(message = "Status is required") String status,
        @JsonProperty("assignee_id") Long assigneeId,
        @Size(max = 100) List<@NotNull Long> taskLabelIds) {}
