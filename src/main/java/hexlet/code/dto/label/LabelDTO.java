package hexlet.code.dto.label;

import java.time.LocalDate;

public record LabelDTO(Long id, String name, LocalDate createdAt) {}
