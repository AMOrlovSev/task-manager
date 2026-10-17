package hexlet.code.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import hexlet.code.TestcontainersConfig;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import hexlet.code.service.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class SecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtEncoder jwtEncoder;

    // ============================ 401 — без токена ============================

    @ParameterizedTest(name = "{0} {1} without token -> 401")
    @MethodSource("protectedRoutes")
    void protectedRouteWithoutTokenReturns401(HttpMethod method, String path, String body)
            throws Exception {
        var requestBuilder = request(method, path);
        if (body != null) {
            requestBuilder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        mockMvc.perform(requestBuilder).andExpect(status().isUnauthorized());
    }

    static Stream<Arguments> protectedRoutes() {
        return Stream.of(
                // users
                Arguments.of(HttpMethod.GET, "/api/users", null),
                Arguments.of(HttpMethod.GET, "/api/users/1", null),
                Arguments.of(
                        HttpMethod.PUT,
                        "/api/users/1",
                        """
                    { "firstName": "X" }"""),
                Arguments.of(HttpMethod.DELETE, "/api/users/1", null),
                // task_statuses — GET открыт, поэтому только write-операции
                Arguments.of(
                        HttpMethod.POST,
                        "/api/task_statuses",
                        """
                    { "name": "X", "slug": "x" }"""),
                Arguments.of(
                        HttpMethod.PUT,
                        "/api/task_statuses/1",
                        """
                    { "name": "Y" }"""),
                Arguments.of(HttpMethod.DELETE, "/api/task_statuses/1", null),
                // tasks
                Arguments.of(HttpMethod.GET, "/api/tasks", null),
                Arguments.of(HttpMethod.GET, "/api/tasks/1", null),
                Arguments.of(
                        HttpMethod.POST,
                        "/api/tasks",
                        """
                    { "title": "X", "status": "draft" }"""),
                Arguments.of(
                        HttpMethod.PUT,
                        "/api/tasks/1",
                        """
                    { "title": "Y" }"""),
                Arguments.of(HttpMethod.DELETE, "/api/tasks/1", null));
    }

    // ============================ 401 — плохой токен ============================

    @Test
    void tokenSignedWithDifferentKeyReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var token = signWithForeignKey(user);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var token = expiredToken(user);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongIssuerReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var now = Instant.now();

        var token =
                jwtEncoder
                        .encode(
                                JwtEncoderParameters.from(
                                        JwtClaimsSet.builder()
                                                .issuer("evil-issuer")
                                                .subject(String.valueOf(user.getId()))
                                                .issuedAt(now)
                                                .expiresAt(now.plusSeconds(3600))
                                                .build()))
                        .getTokenValue();

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsignedTokenWithAlgNoneReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var now = Instant.now();

        var headerJson = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        var payloadJson =
                String.format(
                        "{\"iss\":\"hexlet-task-manager\",\"sub\":\"%d\",\"iat\":%d,\"exp\":%d}",
                        user.getId(), now.getEpochSecond(), now.plusSeconds(3600).getEpochSecond());

        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        var payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        var token = header + "." + payload + ".";

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ============================ публичные маршруты ============================

    @Test
    void welcomeWithoutTokenIsAllowed() throws Exception {
        mockMvc.perform(get("/welcome")).andExpect(status().isOk());
    }

    @Test
    void healthWithoutTokenIsAllowed() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void createUserWithoutTokenIsAllowed() throws Exception {
        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "email": "new@x.com", "password": "qwerty123" }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void loginWithoutTokenIsAllowed() throws Exception {
        mockMvc.perform(
                        post("/api/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "username": "hexlet@example.com", "password": "qwerty" }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void rootWithoutTokenIsNotUnauthorized() throws Exception {
        var status = mockMvc.perform(get("/")).andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    @Test
    void assetsWithoutTokenIsNotUnauthorized() throws Exception {
        var status = mockMvc.perform(get("/assets/x.js")).andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    // ============================ авторизованный доступ ============================

    @Test
    void listUsersWithValidTokenReturns200() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var token = validToken(user);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void updateSelfReturns200() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var token = validToken(user);

        mockMvc.perform(
                        put("/api/users/" + user.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "New" }"""))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSelfReturns204() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        var token = validToken(user);

        mockMvc.perform(
                        delete("/api/users/" + user.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateOtherReturns403() throws Exception {
        var victim = persistUser("victim@x.com", "qwerty123");
        var attacker = persistUser("attacker@x.com", "qwerty123");
        var token = validToken(attacker);

        mockMvc.perform(
                        put("/api/users/" + victim.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "Hacked" }"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOtherReturns403() throws Exception {
        var victim = persistUser("victim@x.com", "qwerty123");
        var attacker = persistUser("attacker@x.com", "qwerty123");
        var token = validToken(attacker);

        mockMvc.perform(
                        delete("/api/users/" + victim.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOtherNonExistentReturns403Not404() throws Exception {
        var attacker = persistUser("attacker@x.com", "qwerty123");
        var token = validToken(attacker);

        // 999999 не существует, но авторизация должна сработать раньше поиска
        mockMvc.perform(
                        put("/api/users/999999")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isForbidden());
    }

    // ============================ сквозной сценарий ============================

    @Test
    void loginThenListUsersWithReturnedToken() throws Exception {
        var token = login("hexlet@example.com", "qwerty");

        assertThat(token).contains(".");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginThenUpdateAndDeleteSelfWithRealToken() throws Exception {
        var admin = userRepository.findByEmail("hexlet@example.com").orElseThrow();
        var token = login("hexlet@example.com", "qwerty");

        mockMvc.perform(
                        put("/api/users/" + admin.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "Updated" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));

        mockMvc.perform(
                        delete("/api/users/" + admin.getId())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ============================ helpers ============================

    private User persistUser(String email, String rawPassword) {
        var user = new User();
        user.setEmail(email);
        user.setPasswordDigest(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private String validToken(User user) {
        var now = Instant.now();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims(user, now, now.plusSeconds(3600))))
                .getTokenValue();
    }

    private String expiredToken(User user) {
        // iat = now - 2h, exp = iat + 1h => exp < now, far past 60s skew of Nimbus
        var past = Instant.now().minusSeconds(7200);
        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims(user, past, past.plusSeconds(3600))))
                .getTokenValue();
    }

    private String login(String username, String password) throws Exception {
        return mockMvc.perform(
                        post("/api/login")
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

    private String signWithForeignKey(User user) throws Exception {
        var pair = generateKeyPair();
        var jwk =
                new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                        .privateKey((RSAPrivateKey) pair.getPrivate())
                        .build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        var now = Instant.now();
        return encoder.encode(JwtEncoderParameters.from(claims(user, now, now.plusSeconds(3600))))
                .getTokenValue();
    }

    private JwtClaimsSet claims(User user, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder()
                .issuer(JwtService.ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private static KeyPair generateKeyPair() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }
}
