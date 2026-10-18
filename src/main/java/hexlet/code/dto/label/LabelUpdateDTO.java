package hexlet.code.dto.label;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Частичное обновление метки")
public class LabelUpdateDTO {

    @Schema(description = "Название метки", example = "feature", minLength = 3, maxLength = 1000)
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 1000, message = "Name must be between 3 and 1000 characters")
    private String name;

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }
}
