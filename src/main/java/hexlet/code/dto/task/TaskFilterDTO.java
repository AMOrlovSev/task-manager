package hexlet.code.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskFilterDTO(
        @Schema(description = "Подстрока в названии, без учёта регистра", example = "create")
                String titleCont,
        @Schema(description = "Идентификатор исполнителя", example = "1") Long assigneeId,
        @Schema(description = "Slug статуса", example = "to_be_fixed") String status,
        @Schema(description = "Идентификатор метки", example = "1") Long labelId) {}
