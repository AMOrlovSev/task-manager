package hexlet.code.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import hexlet.code.TestcontainersConfig;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import hexlet.code.service.JwtService;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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

    @Test
    void listUsersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void getUserByIdWithoutTokenReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        mockMvc.perform(get("/api/users/" + user.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void putWithoutTokenReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        mockMvc.perform(
                        put("/api/users/" + user.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                { "firstName": "X" }"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteWithoutTokenReturns401() throws Exception {
        var user = persistUser("u@x.com", "qwerty123");
        mockMvc.perform(delete("/api/users/" + user.getId())).andExpect(status().isUnauthorized());
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
        var token =
                mockMvc.perform(
                                post("/api/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                { "username": "hexlet@example.com", "password": "qwerty" }
                                """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(token).contains(".");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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

    private String signWithForeignKey(User user) throws Exception {
        var pair = generateKeyPair();
        var jwk =
                new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                        .privateKey((RSAPrivateKey) pair.getPrivate())
                        .build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        var now = Instant.now();
        return encoder
                .encode(JwtEncoderParameters.from(claims(user, now, now.plusSeconds(3600))))
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