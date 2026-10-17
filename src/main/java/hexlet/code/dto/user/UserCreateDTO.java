package hexlet.code.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        String firstName,
        String lastName,
        @NotBlank(message = "Email is required") @Size(max = 255, message = "Email must be at most 255 characters") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password is required") @Size(min = 3, message = "Password must be at least 3 characters") String password) {}
