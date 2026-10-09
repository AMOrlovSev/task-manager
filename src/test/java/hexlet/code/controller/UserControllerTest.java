package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hexlet.code.TestcontainersConfig;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class UserControllerTest {

    private static final String URL = "/api/users";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ============================ POST ============================

    @Test
    void testCreateWithAllFields() throws Exception {
        var body =
                """
                {
                  "email": "john@x.com",
                  "password": "qwerty123",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("john@x.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordDigest").doesNotExist());
    }

    @Test
    void testCreateWithoutNames() throws Exception {
        var body =
                """
                { "email": "john@x.com", "password": "qwerty123" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value(nullValue()))
                .andExpect(jsonPath("$.lastName").value(nullValue()));
    }

    @Test
    void testCreateWithInvalidEmail() throws Exception {
        var body =
                """
                { "email": "not-an-email", "password": "qwerty123" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithShortPassword() throws Exception {
        var body =
                """
                { "email": "john@x.com", "password": "ab" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateWithoutEmail() throws Exception {
        var body =
                """
                { "password": "qwerty123" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateNormalizesEmail() throws Exception {
        var body =
                """
                { "email": "John@X.COM", "password": "qwerty123" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@x.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"john@x.com", "John@x.com", "JOHN@X.com"})
    void testCreateDuplicate(String duplicateEmail) throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "john@x.com", "password": "qwerty123" }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "%s", "password": "qwerty123" }
                                """
                                                .formatted(duplicateEmail)))
                .andExpect(status().isConflict());
    }

    @Test
    void testCreateMalformedJsonReturns400() throws Exception {
        var body =
                """
                { "email": "john@x.com" "password": "qwerty123" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // ============================ GET ============================

    @Test
    void testGetById() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(get(URL + "/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("john@x.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordDigest").doesNotExist());
    }

    @Test
    void testGetAll() throws Exception {
        persistUser("a@x.com", "qwerty123", "A", "A");
        persistUser("b@x.com", "qwerty123", "B", "B");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordDigest").doesNotExist());
    }

    @Test
    void testGetNonExistent() throws Exception {
        mockMvc.perform(get(URL + "/999999")).andExpect(status().isNotFound());
    }

    // ============================ PATCH ============================
    // Правила:
    //   email:    отсутствует → не менять | null → 400 | значение → нормализовать
    //   password: отсутствует → не менять | null → 400 | значение → перекодировать
    //   firstName/lastName: отсутствует → не менять | null → очистить | значение → заменить

    @Test
    void testPatchEmailAndPasswordKeepsNames() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "new@x.com", "password": "newpassword" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@x.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.passwordDigest").doesNotExist());

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("new@x.com");
        assertThat(updated.getFirstName()).isEqualTo("John");
        assertThat(updated.getLastName()).isEqualTo("Doe");
        assertThat(passwordEncoder.matches("newpassword", updated.getPasswordDigest())).isTrue();
    }

    @Test
    void testPatchEmptyBodyKeepsEverything() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@x.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
        assertThat(after.getFirstName()).isEqualTo("John");
        assertThat(after.getLastName()).isEqualTo("Doe");
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPatchFirstNameToNull() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": null }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(nullValue()))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getFirstName()).isNull();
        assertThat(after.getLastName()).isEqualTo("Doe");
    }

    @Test
    void testPatchLastNameToNull() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "lastName": null }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value(nullValue()))
                .andExpect(jsonPath("$.firstName").value("John"));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getLastName()).isNull();
        assertThat(after.getFirstName()).isEqualTo("John");
    }

    @Test
    void testPatchEmailNullReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": null }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPatchPasswordNullReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "password": null }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPatchPasswordAbsentKeepsDigest() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "Johnny" }"""))
                .andExpect(status().isOk());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
        assertThat(after.getFirstName()).isEqualTo("Johnny");
    }

    @Test
    void testPatchEmailAbsentKeepsEmail() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "Johnny" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@x.com"));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPatchShortPassword() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "password": "ab" }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPatchEmptyPassword() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "password": "" }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPatchInvalidEmail() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "not-an-email" }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPatchEmailToDuplicate() throws Exception {
        persistUser("taken@x.com", "qwerty123", null, null);
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "taken@x.com" }"""))
                .andExpect(status().isConflict());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPatchNormalizesEmail() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "New@X.COM" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@x.com"));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void testPatchSameEmailDifferentCaseIsNotDuplicate() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "JOHN@X.COM" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@x.com"));
    }

    @Test
    void testPatchMalformedJsonReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        var body =
                """
                { "email": "x@x.com" "firstName": "Y" }
                """;

        mockMvc.perform(
                        patch(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
        assertThat(after.getFirstName()).isEqualTo("John");
    }

    @Test
    void testPatchNonExistent() throws Exception {
        mockMvc.perform(
                        patch(URL + "/999999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void testPutIsNotSupportedReturns405() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isMethodNotAllowed());
    }

    // ============================ DELETE ============================

    @Test
    void testDelete() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", null, null);

        mockMvc.perform(delete(URL + "/" + user.getId())).andExpect(status().isNoContent());
        mockMvc.perform(get(URL + "/" + user.getId())).andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNonExistent() throws Exception {
        mockMvc.perform(delete(URL + "/999999")).andExpect(status().isNotFound());
    }

    // ============================ helpers ============================

    private User persistUser(String email, String rawPassword, String firstName, String lastName) {
        var user = new User();
        user.setEmail(email);
        user.setPasswordDigest(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
