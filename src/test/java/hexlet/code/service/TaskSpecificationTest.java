package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.TestcontainersConfig;
import hexlet.code.config.JpaConfig;
import hexlet.code.dto.task.TaskFilterDTO;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, JpaConfig.class})
class TaskSpecificationTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskStatusRepository taskStatusRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LabelRepository labelRepository;
    @Autowired private EntityManager entityManager;

    private final TaskSpecification spec = new TaskSpecification();

    @Test
    void emptyFilterReturnsAll() {
        var s = saveStatus("Empty", "empty-all");
        saveTask("A", s, null);
        saveTask("B", s, null);

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO(null, null, null, null)), Sort.by("id"));

        assertThat(result).extracting(Task::getName).containsExactly("A", "B");
    }

    @Test
    void titleContCaseInsensitive() {
        var s = saveStatus("Title", "title-cs");
        saveTask("Create new version", s, null);
        saveTask("Other", s, null);

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO("CREATE", null, null, null)), Sort.by("id"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Create new version");
    }

    @Test
    void titleContEscapesPercent() {
        var s = saveStatus("Pct", "pct");
        saveTask("100% done", s, null);
        saveTask("100 done", s, null);

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO("100%", null, null, null)), Sort.by("id"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("100% done");
    }

    @Test
    void titleContEscapesUnderscore() {
        var s = saveStatus("Und", "und");
        saveTask("a_b", s, null);
        saveTask("axb", s, null);

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO("a_b", null, null, null)), Sort.by("id"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("a_b");
    }

    @Test
    void assigneeFilterMatchesOnlyAssignedTasks() {
        var s = saveStatus("Asg", "asg");
        var u = saveUser("asg-spec@x.com");
        saveTask("Assigned", s, u);
        saveTask("Unassigned", s, null);

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO(null, u.getId(), null, null)), Sort.by("id"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Assigned");
    }

    @Test
    void statusFilterMatchesSlug() {
        var draft = saveStatus("SDraft", "slug-draft");
        var done = saveStatus("SDone", "slug-done");
        saveTask("A", draft, null);
        saveTask("B", done, null);

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO(null, null, "slug-draft", null)),
                        Sort.by("id"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("A");
    }

    @Test
    void labelFilterMatchesLinkedLabel() {
        var s = saveStatus("Lbl", "lbl-spec");
        var label = saveLabel("spec-lbl");
        var otherLabel = saveLabel("other-lbl");

        var withLabel = saveTask("WithLabel", s, null);
        withLabel.getLabels().add(label);
        withLabel.getLabels().add(otherLabel);
        taskRepository.saveAndFlush(withLabel);

        saveTask("WithoutLabel", s, null);

        // Выгружаем persistence context, чтобы findAll реально шёл в БД,
        // а не возвращал задачу с коллекцией labels из памяти.
        entityManager.flush();
        entityManager.clear();

        var result =
                taskRepository.findAll(
                        spec.build(new TaskFilterDTO(null, null, null, label.getId())),
                        Sort.by("id"));

        assertThat(result).hasSize(1);
        var found = result.get(0);
        assertThat(found.getName()).isEqualTo("WithLabel");
        // Метки не должны урезаться до той, по которой фильтровали
        assertThat(found.getLabels()).hasSize(2);
    }

    @Test
    void combinedFilterRequiresAllConditions() {
        var draft = saveStatus("CombDraft", "comb-draft");
        var other = saveStatus("CombOther", "comb-other");
        var u = saveUser("combo-spec@x.com");
        var label = saveLabel("combo-spec-lbl");

        var matching = saveTask("match", draft, u);
        matching.getLabels().add(label);
        taskRepository.saveAndFlush(matching);

        var wrongTitle = saveTask("different", draft, u);
        wrongTitle.getLabels().add(label);
        taskRepository.saveAndFlush(wrongTitle);

        var wrongStatus = saveTask("match", other, u);
        wrongStatus.getLabels().add(label);
        taskRepository.saveAndFlush(wrongStatus);

        var wrongAssignee = saveTask("match", draft, null);
        wrongAssignee.getLabels().add(label);
        taskRepository.saveAndFlush(wrongAssignee);

        var result =
                taskRepository.findAll(
                        spec.build(
                                new TaskFilterDTO("match", u.getId(), "comb-draft", label.getId())),
                        Sort.by("id"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("match");
    }

    // ============================ helpers ============================

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

    private Label saveLabel(String name) {
        var l = new Label();
        l.setName(name);
        return labelRepository.saveAndFlush(l);
    }

    private Task saveTask(String name, TaskStatus status, User assignee) {
        var t = new Task();
        t.setName(name);
        t.setTaskStatus(status);
        t.setAssignee(assignee);
        return taskRepository.saveAndFlush(t);
    }
}
