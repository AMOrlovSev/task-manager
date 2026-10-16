package hexlet.code.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hexlet.code.TestcontainersConfig;
import hexlet.code.config.JpaConfig;
import hexlet.code.model.TaskStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, JpaConfig.class})
class TaskStatusRepositoryTest {

    @Autowired private TaskStatusRepository repository;

    private TaskStatus newStatus(String name, String slug) {
        var status = new TaskStatus();
        status.setName(name);
        status.setSlug(slug);
        return status;
    }

    @Test
    void shouldPopulateIdAndAuditFieldsOnSave() {
        var saved = repository.saveAndFlush(newStatus("Draft", "draft"));

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldRejectDuplicateSlug() {
        repository.saveAndFlush(newStatus("Draft", "draft"));

        assertThatThrownBy(() -> repository.saveAndFlush(newStatus("Other", "draft")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateName() {
        repository.saveAndFlush(newStatus("Draft", "draft"));

        assertThatThrownBy(() -> repository.saveAndFlush(newStatus("Draft", "other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindBySlug() {
        var saved = repository.saveAndFlush(newStatus("Draft", "draft"));

        var found = repository.findBySlug("draft");

        assertThat(found)
                .isPresent()
                .get()
                .extracting(TaskStatus::getId, TaskStatus::getName, TaskStatus::getSlug)
                .containsExactly(saved.getId(), "Draft", "draft");
    }

    @Test
    void shouldReturnEmptyWhenSlugNotFound() {
        assertThat(repository.findBySlug("missing")).isEmpty();
    }
}
