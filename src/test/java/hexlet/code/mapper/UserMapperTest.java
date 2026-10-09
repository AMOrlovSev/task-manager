package hexlet.code.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserDTO;
import hexlet.code.dto.UserUpdateDTO;
import hexlet.code.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    // ============================ toDto ============================

    @Test
    void toDtoMapsAllFields() {
        var user = new User();
        user.setId(42L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@x.com");
        user.setPasswordDigest("$2a$10$hash");
        user.setCreatedAt(LocalDateTime.of(2026, 10, 9, 13, 7));

        UserDTO dto = mapper.toDto(user);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.firstName()).isEqualTo("John");
        assertThat(dto.lastName()).isEqualTo("Doe");
        assertThat(dto.email()).isEqualTo("john@x.com");
        assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 9, 13, 7));
    }

    // ============================ toEntity ============================

    @Test
    void toEntityMapsNameFields() {
        var dto = new UserCreateDTO("John", "Doe", "john@x.com", "qwerty123");

        User user = mapper.toEntity(dto);

        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    void toEntityIgnoresEmail() {
        var dto = new UserCreateDTO("John", "Doe", "john@x.com", "qwerty123");

        User user = mapper.toEntity(dto);

        assertThat(user.getEmail()).isNull();
    }

    @Test
    void toEntityIgnoresProtectedFields() {
        var dto = new UserCreateDTO("John", "Doe", "john@x.com", "qwerty123");

        User user = mapper.toEntity(dto);

        assertThat(user.getId()).isNull();
        assertThat(user.getPasswordDigest()).isNull();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void toEntityHandlesNullNames() {
        var dto = new UserCreateDTO(null, null, "john@x.com", "qwerty123");

        User user = mapper.toEntity(dto);

        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
    }

    // ============================ update ============================

    @Test
    void updateReplacesNames() {
        var user = new User();
        user.setFirstName("Old");
        user.setLastName("Name");

        var dto = new UserUpdateDTO();
        dto.setFirstName("New");
        dto.setLastName("Name2");

        mapper.update(dto, user);

        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name2");
    }

    @Test
    void updateClearsFirstNameOnNull() {
        var user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");

        var dto = new UserUpdateDTO();
        dto.setFirstName(null);
        dto.setLastName("Doe");

        mapper.update(dto, user);

        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    void updateClearsLastNameOnNull() {
        var user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");

        var dto = new UserUpdateDTO();
        dto.setFirstName("John");
        dto.setLastName(null);

        mapper.update(dto, user);

        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isNull();
    }

    @Test
    void updateIgnoresEmail() {
        var user = new User();
        user.setEmail("old@x.com");

        var dto = new UserUpdateDTO();
        dto.setEmail("new@x.com");
        dto.setFirstName("John");

        mapper.update(dto, user);

        assertThat(user.getEmail()).isEqualTo("old@x.com");
    }

    @Test
    void updateIgnoresProtectedFields() {
        var user = new User();
        user.setId(42L);
        user.setPasswordDigest("$2a$10$old");
        user.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        user.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));

        var dto = new UserUpdateDTO();
        dto.setFirstName("John");
        dto.setPassword("newpassword");

        mapper.update(dto, user);

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getPasswordDigest()).isEqualTo("$2a$10$old");
        assertThat(user.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
        assertThat(user.getUpdatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    @Test
    void updateDoesNotTouchAnythingWhenAllFieldsNull() {
        var user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@x.com");
        user.setPasswordDigest("$2a$10$hash");

        var dto = new UserUpdateDTO();

        mapper.update(dto, user);

        assertThat(user.getEmail()).isEqualTo("john@x.com");
        assertThat(user.getPasswordDigest()).isEqualTo("$2a$10$hash");
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
    }
}