package hexlet.code.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hexlet.code.TestcontainersConfig;
import hexlet.code.config.JpaConfig;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import jakarta.persistence.EntityManager;
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
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskStatusRepository taskStatusRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private LabelRepository labelRepository;

    @Test
    void existsByAssigneeIdAndStatusId() {
        var status = saveStatus("draft-r", "draft-r");
        var user = saveUser("u@x.com");
        saveTask("T", status, user);

        assertThat(taskRepository.existsByAssigneeId(user.getId())).isTrue();
        assertThat(taskRepository.existsByTaskStatusId(status.getId())).isTrue();
    }

    @Test
    void existsByReturnsFalseWhenNotUsed() {
        assertThat(taskRepository.existsByAssigneeId(999999L)).isFalse();
        assertThat(taskRepository.existsByTaskStatusId(999999L)).isFalse();
    }

    @Test
    void auditFieldsPopulatedOnSave() {
        var status = saveStatus("s1", "s1");
        var saved = saveTask("T", status, null);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(saved.getUpdatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void foreignKeyOnStatusRejectsDelete() {
        var status = saveStatus("s2", "s2");
        saveTask("T", status, null);

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(
                        () -> {
                            taskStatusRepository.deleteById(status.getId());
                            taskStatusRepository.flush();
                        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeyOnAssigneeRejectsDelete() {
        var status = saveStatus("s3", "s3");
        var user = saveUser("x@x.com");
        saveTask("T", status, user);

        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(
                        () -> {
                            userRepository.deleteById(user.getId());
                            userRepository.flush();
                        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByLabelsId() {
        var status = saveStatus("s-lbl", "s-lbl");
        var label = saveLabel("lbl");
        var task = saveTask("T", status, null);
        task.getLabels().add(label);
        taskRepository.saveAndFlush(task);

        assertThat(taskRepository.existsByLabelsId(label.getId())).isTrue();
    }

    @Test
    void existsByLabelsIdReturnsFalseWhenNotLinked() {
        assertThat(taskRepository.existsByLabelsId(999999L)).isFalse();
    }

    @Test
    void deleteTaskRemovesJoinRowsButKeepsLabel() {
        var status = saveStatus("s-del", "s-del");
        var label = saveLabel("lbl-del");
        var task = saveTask("T", status, null);
        task.getLabels().add(label);
        taskRepository.saveAndFlush(task);

        taskRepository.delete(task);
        taskRepository.flush();

        assertThat(labelRepository.findById(label.getId())).isPresent();
        assertThat(taskRepository.existsByLabelsId(label.getId())).isFalse();
    }

    // helpers

    private TaskStatus saveStatus(String name, String slug) {
        var s = new TaskStatus();
        s.setName(name);
        s.setSlug(slug);
        return taskStatusRepository.saveAndFlush(s);
    }

    private User saveUser(String email) {
        var u = new User();
        u.setEmail(email);
        u.setPasswordDigest("$2a$10$hash");
        return userRepository.saveAndFlush(u);
    }

    private Task saveTask(String name, TaskStatus status, User assignee) {
        var t = new Task();
        t.setName(name);
        t.setTaskStatus(status);
        t.setAssignee(assignee);
        return taskRepository.saveAndFlush(t);
    }

    private Label saveLabel(String name) {
        var l = new Label();
        l.setName(name);
        return labelRepository.saveAndFlush(l);
    }
}
