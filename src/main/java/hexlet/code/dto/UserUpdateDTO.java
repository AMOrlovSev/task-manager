package hexlet.code.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@Setter
public class UserUpdateDTO {
    @Valid private JsonNullable<
                    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String>
            email = JsonNullable.undefined();

    @Valid private JsonNullable<
                    @NotBlank(message = "Password is required") @Size(min = 3, message = "Password must be at least 3 characters") String>
            password = JsonNullable.undefined();

    private JsonNullable<String> firstName = JsonNullable.undefined();
    private JsonNullable<String> lastName = JsonNullable.undefined();
}
