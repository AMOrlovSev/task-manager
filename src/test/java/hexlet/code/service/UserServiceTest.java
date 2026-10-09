package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hexlet.code.dto.UserCreateDTO;
import hexlet.code.dto.UserDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.UserMapperImpl;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    private UserMapperImpl userMapper;
    private PasswordEncoder passwordEncoder;
    private JsonMapper jsonMapper;
    private Validator validator;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapperImpl();
        passwordEncoder = new BCryptPasswordEncoder();

        // Те же настройки, что Spring Boot применяет к своему JsonMapper по умолчанию:
        // неизвестные поля игнорируются.
        jsonMapper =
                JsonMapper.builder()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .build();

        validator = Validation.buildDefaultValidatorFactory().getValidator();

        userService =
                new UserService(userRepository, userMapper, passwordEncoder, jsonMapper, validator);
    }

    // ============================ findAll ============================

    @Test
    void findAllReturnsMappedPage() {
        var user1 = existingUser(1L, "a@x.com", "qwerty123", "A", "A");
        var user2 = existingUser(2L, "b@x.com", "qwerty123", "B", "B");
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user1, user2), pageable, 2));

        Page<UserDTO> page = userService.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(UserDTO::email)
                .containsExactly("a@x.com", "b@x.com");

        assertThat(page.getContent()).allSatisfy(dto -> assertThat(dto.id()).isNotNull());
    }

    // ============================ findById ============================

    @Test
    void findByIdReturnsDto() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDTO dto = userService.findById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.email()).isEqualTo("john@x.com");
        assertThat(dto.firstName()).isEqualTo("John");
        assertThat(dto.lastName()).isEqualTo("Doe");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ============================ create ============================

    @Test
    void createNormalizesEmailAndEncodesPassword() {
        var dto = new UserCreateDTO("John", "Doe", "  John@X.COM  ", "qwerty123");
        when(userRepository.existsByEmail("john@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO created = userService.create(dto);

        assertThat(created.email()).isEqualTo("john@x.com");
        assertThat(created.firstName()).isEqualTo("John");
        assertThat(created.lastName()).isEqualTo("Doe");

        var saved = captureSavedUser();
        assertThat(saved.getEmail()).isEqualTo("john@x.com");
        assertThat(saved.getPasswordDigest()).isNotEqualTo("qwerty123");
        assertThat(passwordEncoder.matches("qwerty123", saved.getPasswordDigest())).isTrue();
    }

    @Test
    void createThrowsWhenEmailExists() {
        var dto = new UserCreateDTO("John", "Doe", "john@x.com", "qwerty123");
        when(userRepository.existsByEmail("john@x.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("john@x.com");

        verify(userRepository, never()).save(any());
    }

    // ============================ update ============================

    @Test
    void updateEmptyPatchKeepsEverything() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.update(1L, patch("{}"));

        assertThat(result.email()).isEqualTo("john@x.com");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(user.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void updateEmailAndPasswordKeepsNames() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result =
                userService.update(
                        1L,
                        patch(
                                """
                { "email": "new@x.com", "password": "newpassword" }
                """));

        assertThat(result.email()).isEqualTo("new@x.com");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(passwordEncoder.matches("newpassword", user.getPasswordDigest())).isTrue();
    }

    @Test
    void updateFirstNameToNullClearsIt() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update(
                1L,
                patch(
                        """
                { "firstName": null }"""));

        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    void updateEmailToNullFails() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        1L,
                                        patch(
                                                """
                { "email": null }""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("email"));

        assertThat(user.getEmail()).isEqualTo("john@x.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePasswordToNullFails() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        1L,
                                        patch(
                                                """
                { "password": null }""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("password"));

        assertThat(user.getPasswordDigest()).isEqualTo(digestBefore);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePasswordAbsentKeepsDigest() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.update(
                1L,
                patch(
                        """
                { "firstName": "Johnny" }"""));

        assertThat(user.getPasswordDigest()).isEqualTo(digestBefore);
        assertThat(user.getFirstName()).isEqualTo("Johnny");
    }

    @Test
    void updateShortPasswordFails() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        1L,
                                        patch(
                                                """
                { "password": "ab" }""")))
                .isInstanceOf(ValidationException.class);

        assertThat(user.getPasswordDigest()).isEqualTo(digestBefore);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmptyPasswordFails() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        1L,
                                        patch(
                                                """
                { "password": "" }""")))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateInvalidEmailFails() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        1L,
                                        patch(
                                                """
                { "email": "not-an-email" }""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("email"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmailToDuplicateFails() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@x.com")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        1L,
                                        patch(
                                                """
                { "email": "taken@x.com" }""")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("taken@x.com");

        assertThat(user.getEmail()).isEqualTo("john@x.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateNormalizesEmail() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result =
                userService.update(
                        1L,
                        patch(
                                """
                { "email": "NEW@X.COM" }"""));

        assertThat(result.email()).isEqualTo("new@x.com");
        assertThat(user.getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void updateSameEmailDifferentCaseIsNotDuplicate() {
        var user = existingUser(1L, "john@x.com", "qwerty123", "John", "Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result =
                userService.update(
                        1L,
                        patch(
                                """
                { "email": "JOHN@X.COM" }"""));

        assertThat(result.email()).isEqualTo("john@x.com");
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateNonExistentUserThrows() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                userService.update(
                                        999L,
                                        patch(
                                                """
                { "firstName": "X" }""")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ============================ delete ============================

    @Test
    void deleteExistingUser() {
        var user = existingUser(1L, "john@x.com", "qwerty123", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteNonExistentUserThrows() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    // ============================ helpers ============================

    private User existingUser(
            Long id, String email, String rawPassword, String firstName, String lastName) {
        var user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordDigest(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private JsonNode patch(String json) {
        return jsonMapper.readTree(json);
    }

    private User captureSavedUser() {
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }
}
