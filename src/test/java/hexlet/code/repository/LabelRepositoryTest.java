package hexlet.code.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hexlet.code.TestcontainersConfig;
import hexlet.code.config.JpaConfig;
import hexlet.code.model.Label;
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
class LabelRepositoryTest {

    @Autowired private LabelRepository repository;

    private Label newLabel(String name) {
        var label = new Label();
        label.setName(name);
        return label;
    }

    @Test
    void shouldPopulateIdAndAuditFieldsOnSave() {
        var saved = repository.saveAndFlush(newLabel("backend"));

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(saved.getUpdatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldRejectDuplicateName() {
        repository.saveAndFlush(newLabel("duplicated"));

        assertThatThrownBy(() -> repository.saveAndFlush(newLabel("duplicated")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindByName() {
        var saved = repository.saveAndFlush(newLabel("feature"));

        var found = repository.findByName("feature");

        assertThat(found)
                .isPresent()
                .get()
                .extracting(Label::getId, Label::getName)
                .containsExactly(saved.getId(), "feature");
    }

    @Test
    void shouldReturnEmptyWhenNameNotFound() {
        assertThat(repository.findByName("missing")).isEmpty();
    }

    @Test
    void shouldCheckExistsByName() {
        repository.saveAndFlush(newLabel("feature"));

        assertThat(repository.existsByName("feature")).isTrue();
        assertThat(repository.existsByName("missing")).isFalse();
    }
}
