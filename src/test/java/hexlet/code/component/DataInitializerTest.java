package hexlet.code.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import hexlet.code.TestcontainersConfig;
import hexlet.code.config.AdminProperties;
import hexlet.code.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Import(TestcontainersConfig.class)
class DataInitializerTest {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DataInitializer initializer;
    @Autowired private AdminProperties adminProperties;

    @Test
    void adminIsCreatedOnStartup() {
        var admin = userRepository.findByEmail(adminProperties.email()).orElseThrow();
        assertThat(passwordEncoder.matches(adminProperties.password(), admin.getPasswordDigest()))
                .isTrue();
        assertThat(admin.getFirstName()).isNull();
        assertThat(admin.getLastName()).isNull();
    }

    @Test
    void secondRunDoesNotFailOrDuplicate() {
        var before = userRepository.count();

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        assertThat(userRepository.count()).isEqualTo(before);
    }
}
