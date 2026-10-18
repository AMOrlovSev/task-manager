package hexlet.code.dto.label;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для создания метки")
public record LabelCreateDTO(
        @Schema(description = "Название метки", example = "bug", minLength = 3, maxLength = 1000)
                @NotBlank(message = "Name is required")
                @Size(min = 3, max = 1000, message = "Name must be between 3 and 1000 characters")
                String name) {

    public LabelCreateDTO {
        name = name == null ? null : name.strip();
    }
}
