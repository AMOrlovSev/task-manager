package hexlet.code.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelCreateDTO(
        @NotBlank(message = "Name is required") @Size(min = 3, max = 1000, message = "Name must be between 3 and 1000 characters") String name) {

    public LabelCreateDTO {
        name = name == null ? null : name.strip();
    }
}
