package hexlet.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hexlet.code.TestcontainersConfig;
import hexlet.code.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class AuthControllerTest {

    private static final String URL = "/api/login";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtDecoder jwtDecoder;

    // ============================ 200 ============================

    @Test
    void loginAsAdminReturnsJwt() throws Exception {
        var admin = userRepository.findByEmail("hexlet@example.com").orElseThrow();

        var token = login("hexlet@example.com", "qwerty");

        assertThat(token).matches("[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+");

        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(admin.getId().toString());
    }

    @Test
    void loginWithUppercaseEmailWorks() throws Exception {
        var admin = userRepository.findByEmail("hexlet@example.com").orElseThrow();

        var token = login("HEXLET@EXAMPLE.COM", "qwerty");

        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(admin.getId().toString());
    }

    // ============================ 401 ============================

    @Test
    void wrongPasswordReturns401() throws Exception {
        expectUnauthorized(
                """
                { "username": "hexlet@example.com", "password": "wrong" }
                """);
    }

    @Test
    void unknownEmailReturns401() throws Exception {
        expectUnauthorized(
                """
                { "username": "nobody@example.com", "password": "qwerty" }
                """);
    }

    @Test
    void emptyFieldsReturn401() throws Exception {
        expectUnauthorized(
                """
                { "username": "", "password": "" }
                """);
    }

    @Test
    void emptyBodyReturns401() throws Exception {
        expectUnauthorized("{}");
    }

    @Test
    void all401ResponsesHaveIdenticalBody() throws Exception {
        var wrongPassword =
                rawUnauthorized(
                        """
                        { "username": "hexlet@example.com", "password": "wrong" }
                        """);
        var unknownEmail =
                rawUnauthorized(
                        """
                        { "username": "nobody@example.com", "password": "qwerty" }
                        """);
        var emptyFields =
                rawUnauthorized(
                        """
                        { "username": "", "password": "" }
                        """);
        var emptyBody = rawUnauthorized("{}");

        assertThat(wrongPassword).isNotBlank();
        assertThat(wrongPassword)
                .isEqualTo(unknownEmail)
                .isEqualTo(emptyFields)
                .isEqualTo(emptyBody);
    }

    // ============================ 400 ============================

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "username": "hexlet@example.com" "password": "qwerty" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============================ helpers ============================

    private String login(String username, String password) throws Exception {
        return mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "username": "%s", "password": "%s" }
                                """
                                                .formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void expectUnauthorized(String body) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    private String rawUnauthorized(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
