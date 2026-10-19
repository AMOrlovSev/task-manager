package hexlet.code.repository;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.TestcontainersConfig;
import hexlet.code.dto.task.TaskFilterDTO;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.service.TaskService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfig.class)
@Transactional
class TaskQueryCountTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskStatusRepository taskStatusRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LabelRepository labelRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private JsonMapper jsonMapper;

    @Test
    void findAllWithEntityGraphUsesSingleQuery() {
        var statusA = saveStatus("QueryA", "query-a");
        var statusB = saveStatus("QueryB", "query-b");
        var user = saveUser("qcount@x.com");
        var label1 = saveLabel("qcount-l1");
        var label2 = saveLabel("qcount-l2");

        for (int i = 0; i < 10; i++) {
            var task = new Task();
            task.setName("Task " + i);
            task.setTaskStatus(i % 2 == 0 ? statusA : statusB);
            task.setAssignee(i % 3 == 0 ? null : user);
            task.getLabels().add(i % 2 == 0 ? label1 : label2);
            taskRepository.save(task);
        }
        taskRepository.flush();

        entityManager.clear();

        statistics().clear();

        var result = taskService.findAll(new TaskFilterDTO(null, null, null, null));

        assertThat(result).hasSize(10);
        assertThat(statistics().getPrepareStatementCount())
                .as("@EntityGraph должен собрать taskStatus и labels одним запросом")
                .isEqualTo(1);
    }

    @Test
    void findByIdLoadsTaskWithDetailsInSingleQuery() {
        var status = taskStatusRepository.findBySlug("draft").orElseThrow();
        var assignee = saveUser("query-count-find@example.com");
        var label1 = saveLabel("label-find-1");
        var label2 = saveLabel("label-find-2");

        var task = saveTask("Task for findById", status, assignee, Set.of(label1, label2));
        task = taskRepository.saveAndFlush(task);

        entityManager.clear();
        statistics().clear();

        var dto = taskService.findById(task.getId());

        assertThat(dto.taskLabelIds()).hasSize(2);
        assertThat(statistics().getPrepareStatementCount())
                .as("один SELECT задачи с taskStatus и labels через @EntityGraph")
                .isEqualTo(1);
    }

    @Test
    void updateLoadsTaskWithDetailsAndUpdatesInTwoQueries() {
        var status = taskStatusRepository.findBySlug("draft").orElseThrow();
        var assignee = saveUser("query-count-update@example.com");
        var label1 = saveLabel("label-update-1");
        var label2 = saveLabel("label-update-2");

        var task = saveTask("Task for update", status, assignee, Set.of(label1, label2));
        task = taskRepository.saveAndFlush(task);

        entityManager.clear();
        statistics().clear();

        taskService.update(
                task.getId(),
                jsonMapper.readTree(
                        """
                {"title": "New"}"""));
        entityManager.flush();

        // 1 — SELECT задачи с taskStatus и labels через @EntityGraph,
        // 2 — UPDATE tasks после изменения title.
        assertThat(statistics().getPrepareStatementCount())
                .as("загрузка с @EntityGraph + UPDATE")
                .isEqualTo(2);
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private Task saveTask(String name, TaskStatus status, User assignee, Set<Label> labels) {
        var task = new Task();
        task.setName(name);
        task.setTaskStatus(status);
        task.setAssignee(assignee);
        task.getLabels().addAll(labels);
        return task;
    }

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
}
