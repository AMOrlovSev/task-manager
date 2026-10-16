package hexlet.code.dto.task_status;

import jakarta.validation.constraints.NotBlank;

public record TaskStatusCreateDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Slug is required") String slug) {}
