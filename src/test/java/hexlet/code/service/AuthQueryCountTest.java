package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.TestcontainersConfig;
import hexlet.code.model.User;
import hexlet.code.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfig.class)
@Transactional
class AuthQueryCountTest {

    private static final String RAW_PASSWORD = "qwerty";

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void loginLoadsUserInSingleQuery() {
        var email = "auth-qcount@example.com";
        var user = new User();
        user.setEmail(email);
        user.setPasswordDigest(passwordEncoder.encode(RAW_PASSWORD));
        userRepository.saveAndFlush(user);

        entityManager.clear();
        statistics().clear();

        var token = authService.login(email, RAW_PASSWORD);

        assertThat(token).isNotBlank();
        assertThat(statistics().getPrepareStatementCount())
                .as(
                        "один SELECT users в DbUserDetailsService; второго запроса в AuthService больше нет")
                .isEqualTo(1);
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
