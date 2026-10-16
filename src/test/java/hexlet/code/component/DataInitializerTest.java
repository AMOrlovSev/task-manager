package hexlet.code.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import hexlet.code.TestcontainersConfig;
import hexlet.code.config.AdminProperties;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
    @Autowired private TaskStatusRepository taskStatusRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private DataInitializer initializer;
    @Autowired private AdminProperties adminProperties;

    @AfterEach
    void restoreRenamedStatus() {
        taskStatusRepository
                .findBySlug("custom-draft")
                .ifPresent(
                        status -> {
                            status.setSlug("draft");
                            taskStatusRepository.saveAndFlush(status);
                        });
    }

    @Test
    void adminIsCreatedOnStartup() {
        var admin = userRepository.findByEmail(adminProperties.email()).orElseThrow();
        assertThat(passwordEncoder.matches(adminProperties.password(), admin.getPasswordDigest()))
                .isTrue();
        assertThat(admin.getFirstName()).isNull();
        assertThat(admin.getLastName()).isNull();
    }

    @Test
    void secondRunDoesNotFailOrDuplicateUsers() {
        var before = userRepository.count();

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void adminUsesDevelopmentDefaults() {
        var admin = userRepository.findByEmail("hexlet@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("qwerty", admin.getPasswordDigest())).isTrue();
    }

    @Test
    void defaultTaskStatusesAreSeeded() {
        var expectedSlugs = List.of("draft", "to_review", "to_be_fixed", "to_publish", "published");
        for (String slug : expectedSlugs) {
            assertThat(taskStatusRepository.findBySlug(slug))
                    .as("slug '%s' should be present after startup", slug)
                    .isPresent();
        }
    }

    @Test
    void secondRunDoesNotDuplicateTaskStatuses() {
        var before = taskStatusRepository.count();

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        assertThat(taskStatusRepository.count()).isEqualTo(before);
    }

    @Test
    void renamedSlugDoesNotBreakStartup() {
        var draft = taskStatusRepository.findBySlug("draft").orElseThrow();
        draft.setSlug("custom-draft");
        taskStatusRepository.save(draft);

        var before = taskStatusRepository.count();

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        assertThat(taskStatusRepository.count()).isEqualTo(before);
    }
}
