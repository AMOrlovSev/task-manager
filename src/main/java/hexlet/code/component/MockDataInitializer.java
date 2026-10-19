package hexlet.code.component;

import hexlet.code.dto.label.LabelCreateDTO;
import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.service.LabelService;
import hexlet.code.service.TaskService;
import hexlet.code.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.mock-data.enabled", havingValue = "true", matchIfMissing = false)
public class MockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataInitializer.class);

    private final UserService userService;
    private final LabelService labelService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;

    public MockDataInitializer(
            UserService userService,
            LabelService labelService,
            TaskService taskService,
            TaskRepository taskRepository,
            TaskStatusRepository taskStatusRepository) {
        this.userService = userService;
        this.labelService = labelService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.taskStatusRepository = taskStatusRepository;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (taskRepository.count() > 0) {
            log.info("Tasks already exist, skipping mock data generation.");
            return;
        }

        log.info("Generating mock data for development...");
        Random random = new Random();

        List<Long> userIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String email = "user" + i + "@example.com";
            try {
                var created =
                        userService.create(
                                new UserCreateDTO(
                                        "User" + i, "Lastname" + i, email, "password123"));
                userIds.add(created.id());
            } catch (Exception e) {
                // Игнорируем, если уже существует
            }
        }

        List<Long> labelIds = new ArrayList<>();
        String[] labelNames = {
            "frontend", "backend", "bug", "feature", "urgent",
            "low-priority", "testing", "design", "docs", "devops"
        };
        for (String name : labelNames) {
            try {
                var created = labelService.create(new LabelCreateDTO(name));
                labelIds.add(created.id());
            } catch (Exception e) {
                // Игнорируем, если уже существует
            }
        }

        List<String> statusSlugs =
                taskStatusRepository.findAll().stream().map(TaskStatus::getSlug).toList();

        for (int i = 1; i <= 30; i++) {
            String title = "Mock Task " + i + " - " + generateRandomWord(random);
            String content =
                    "This is a generated description for task "
                            + i
                            + ". It helps to test pagination.";
            Integer index = i * 10;

            String slug = statusSlugs.get(random.nextInt(statusSlugs.size()));

            Long assigneeId = (i % 5 == 0) ? null : userIds.get(random.nextInt(userIds.size()));

            List<Long> taskLabels = new ArrayList<>();
            if (!labelIds.isEmpty()) {
                taskLabels.add(labelIds.get(random.nextInt(labelIds.size())));
                if (i % 2 == 0 && labelIds.size() > 1) {
                    Long secondLabel = labelIds.get(random.nextInt(labelIds.size()));
                    if (!secondLabel.equals(taskLabels.get(0))) {
                        taskLabels.add(secondLabel);
                    }
                }
            }

            try {
                taskService.create(
                        new TaskCreateDTO(title, content, index, slug, assigneeId, taskLabels));
            } catch (Exception e) {
                log.warn("Failed to create mock task {}", i, e);
            }
        }

        log.info("Successfully generated 30 mock tasks, 5 users, and 10 labels.");
    }

    private String generateRandomWord(Random random) {
        String[] words = {
            "Refactor", "Fix", "Implement", "Update", "Design", "Review", "Deploy", "Test"
        };
        return words[random.nextInt(words.length)];
    }
}
