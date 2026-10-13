package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
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

        mockMvc.perform(get(URL + "/" + user.getId()).with(authAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("john@x.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordDigest").doesNotExist());
    }

    @Test
    void testGetNonExistent() throws Exception {
        var viewer = persistUser("viewer@x.com", "qwerty123", null, null);

        mockMvc.perform(get(URL + "/999999").with(authAs(viewer))).andExpect(status().isNotFound());
    }

    @Test
    void testGetAll() throws Exception {
        var before = userRepository.count();

        persistUser("a@x.com", "qwerty123", "A", "A");
        var auth = persistUser("b@x.com", "qwerty123", "B", "B");

        mockMvc.perform(get(URL).with(authAs(auth)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(before + 2)))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(before + 2))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].passwordDigest").doesNotExist());
    }

    @Test
    void testGetAllReturnsCurrentUsers() throws Exception {
        var viewer = persistUser("viewer@x.com", "qwerty123", null, null);
        var before = userRepository.count();

        mockMvc.perform(get(URL).with(authAs(viewer)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(before)))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(before));
    }

    @Test
    void testGetUsersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    // ============================ PUT ============================
    // Правила:
    //   email:    отсутствует → не менять | null → 400 | значение → нормализовать
    //   password: отсутствует → не менять | null → 400 | значение → перекодировать
    //   firstName/lastName: отсутствует → не менять | null → очистить | значение → заменить

    @Test
    void testPutEmailAndPasswordKeepsNames() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutEmptyBodyKeepsEverything() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutFirstNameToNull() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutLastNameToNull() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutEmailNullReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": null }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPutPasswordNullReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "password": null }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPutPasswordAbsentKeepsDigest() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutEmailAbsentKeepsEmail() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutShortPassword() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "password": "ab" }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPutEmptyPassword() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "password": "" }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
    }

    @Test
    void testPutInvalidEmail() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "not-an-email" }"""))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPutEmailToDuplicate() throws Exception {
        persistUser("taken@x.com", "qwerty123", null, null);
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "taken@x.com" }"""))
                .andExpect(status().isConflict());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
    }

    @Test
    void testPutNormalizesEmail() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
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
    void testPutSameEmailDifferentCaseIsNotDuplicate() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "JOHN@X.COM" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@x.com"));
    }

    @Test
    void testPutMalformedJsonReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        var body =
                """
                { "email": "x@x.com" "firstName": "Y" }
                """;

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getEmail()).isEqualTo("john@x.com");
        assertThat(after.getFirstName()).isEqualTo("John");
    }

    @Test
    void testPutNonExistentReturns404WhenSubjectMatches() throws Exception {
        var token = authAsAnonymous();

        mockMvc.perform(
                        put(URL + "/999999")
                                .with(token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void testPutIgnoresIdAndPasswordDigestInBody() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");
        var digestBefore = user.getPasswordDigest();

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "id": 999, "passwordDigest": "hacked", "firstName": "X" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.firstName").value("X"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getId()).isEqualTo(user.getId());
        assertThat(after.getPasswordDigest()).isEqualTo(digestBefore);
        assertThat(passwordEncoder.matches("qwerty123", after.getPasswordDigest())).isTrue();
    }

    @Test
    void testPutArrayBodyReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[1, 2, 3]"))
                .andExpect(status().isBadRequest());

        var after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(after.getFirstName()).isEqualTo("John");
    }

    @Test
    void testPutStringBodyReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("\"x\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPutNullBodyReturns400() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .with(authAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateOtherReturns403() throws Exception {
        var victim = persistUser("victim@x.com", "qwerty123", "V", "V");
        var attacker = persistUser("attacker@x.com", "qwerty123", "A", "A");

        mockMvc.perform(
                        put(URL + "/" + victim.getId())
                                .with(authAs(attacker))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "Hacked" }"""))
                .andExpect(status().isForbidden());

        var after = userRepository.findById(victim.getId()).orElseThrow();
        assertThat(after.getFirstName()).isEqualTo("V");
    }

    @Test
    void testUpdateOthersNonExistentReturns403Not404() throws Exception {
        var attacker = persistUser("attacker@x.com", "qwerty123", "A", "A");

        mockMvc.perform(
                        put(URL + "/999999")
                                .with(authAs(attacker))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPutWithoutTokenReturns401() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(
                        put(URL + "/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isUnauthorized());
    }

    // ============================ DELETE ============================

    @Test
    void testDelete() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", null, null);

        mockMvc.perform(delete(URL + "/" + user.getId()).with(authAs(user)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(URL + "/" + user.getId()).with(authAs(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNonExistent() throws Exception {
        mockMvc.perform(delete(URL + "/999999").with(authAsAnonymous()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteOtherReturns403() throws Exception {
        var victim = persistUser("victim@x.com", "qwerty123", "V", "V");
        var attacker = persistUser("attacker@x.com", "qwerty123", "A", "A");

        mockMvc.perform(delete(URL + "/" + victim.getId()).with(authAs(attacker)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(victim.getId())).isPresent();
    }

    @Test
    void testDeleteWithoutTokenReturns401() throws Exception {
        var user = persistUser("john@x.com", "qwerty123", "John", "Doe");

        mockMvc.perform(delete(URL + "/" + user.getId())).andExpect(status().isUnauthorized());
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

    private RequestPostProcessor authAs(User user) {
        return jwt().jwt(j -> j.subject(String.valueOf(user.getId())));
    }

    private RequestPostProcessor authAsAnonymous() {
        return jwt().jwt(j -> j.subject("999999"));
    }
}
