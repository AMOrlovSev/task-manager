package hexlet.code.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record TaskDTO(
        Long id,
        Integer index,
        LocalDate createdAt,
        @JsonProperty("assignee_id") Long assigneeId,
        String title,
        String content,
        String status,
        List<Long> taskLabelIds) {}
