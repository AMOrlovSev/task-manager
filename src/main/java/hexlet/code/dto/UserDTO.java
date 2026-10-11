package hexlet.code.dto;

import java.time.LocalDate;

public record UserDTO(
        Long id, String firstName, String lastName, String email, LocalDate createdAt) {}
