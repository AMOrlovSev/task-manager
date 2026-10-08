package hexlet.code.dto;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDateTime createdAt) {
}
