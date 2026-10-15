package hexlet.code.dto.task_status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskStatusCreateDTO(
        @NotBlank(message = "Name is required") @Size(min = 1, message = "Name must be at least 1 character") String name,
        @NotBlank(message = "Slug is required") @Size(min = 1, message = "Slug must be at least 1 character") String slug) {}
