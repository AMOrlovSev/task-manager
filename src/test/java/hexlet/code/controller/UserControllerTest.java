package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class UserControllerTest {

    private static final String URL = "/api/users";

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    // ---------- POST ----------

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
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordDigest").doesNotExist());
    }

    @Test
    void testCreateWithoutNames() throws Exception {
        var body =
                """
                {
                  "email": "john@x.com",
                  "password": "qwerty123"
                }
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

    @ParameterizedTest
    @ValueSource(strings = {"john@x.com", "John@x.com", "JOHN@X.com"})
    void testCreateDuplicate(String duplicateEmail) throws Exception {
        var first =
                """
                { "email": "john@x.com", "password": "qwerty123" }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isCreated());

        var second =
                """
                { "email": "%s", "password": "qwerty123" }
                """
                        .formatted(duplicateEmail);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isConflict());
    }

    // ---------- GET ----------

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
        persistUser("a@x.com", "qwerty123", null, null);
        persistUser("b@x.com", "qwerty123", null, null);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].passwordDigest").doesNotExist());
    }

    @Test
    void testGetNonExistent() throws Exception {
        mockMvc.perform(get(URL + "/999999")).andExpect(status().isNotFound());
    }

    // ---------- PUT ----------

    @Test
    void testUpdateEmailAndPasswordKeepsNames() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        var body =
                """
                {
                  "email": "new@x.com",
                  "password": "newpassword"
                }
                """;

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@x.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.passwordDigest").doesNotExist());

        var updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("John");
        assertThat(updated.getLastName()).isEqualTo("Doe");
        assertThat(passwordEncoder.matches("newpassword", updated.getPasswordDigest())).isTrue();
    }

    @Test
    void testUpdateEmptyBodyKeepsEverything() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
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
    void testUpdateFirstNameToNull() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        var body =
                """
                { "firstName": null }
                """;

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(nullValue()));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getFirstName()).isNull();
        assertThat(after.getLastName()).isEqualTo("Doe");
    }

    @Test
    void testUpdateEmailNull() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", null, null);

        var body =
                """
                { "email": null }
                """;

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateShortPassword() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", null, null);

        var body =
                """
                { "password": "ab" }
                """;

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateNonExistent() throws Exception {
        var body =
                """
                { "firstName": "X" }
                """;

        mockMvc.perform(put(URL + "/999999").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateEmailNullReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", null, null);
        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                            { "email": null }
                            """))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE ----------

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

    // ---------- helpers ----------

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
