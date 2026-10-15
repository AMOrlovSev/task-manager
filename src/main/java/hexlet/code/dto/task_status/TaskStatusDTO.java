package hexlet.code.dto.task_status;

import java.time.LocalDate;

public record TaskStatusDTO(
        Long id, String name, String slug, LocalDate createdAt, LocalDate updatedAt) {}
