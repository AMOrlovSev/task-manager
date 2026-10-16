package hexlet.code.component;

import hexlet.code.config.AdminProperties;
import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.service.TaskStatusService;
import hexlet.code.service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final List<TaskStatusCreateDTO> DEFAULT_STATUSES =
            List.of(
                    new TaskStatusCreateDTO("Draft", "draft"),
                    new TaskStatusCreateDTO("ToReview", "to_review"),
                    new TaskStatusCreateDTO("ToBeFixed", "to_be_fixed"),
                    new TaskStatusCreateDTO("ToPublish", "to_publish"),
                    new TaskStatusCreateDTO("Published", "published"));

    private final AdminProperties admin;
    private final UserService userService;
    private final TaskStatusService taskStatusService;
    private final TaskStatusRepository taskStatusRepository;

    public DataInitializer(
            AdminProperties admin,
            UserService userService,
            TaskStatusService taskStatusService,
            TaskStatusRepository taskStatusRepository) {
        this.admin = admin;
        this.userService = userService;
        this.taskStatusService = taskStatusService;
        this.taskStatusRepository = taskStatusRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            userService.create(new UserCreateDTO(null, null, admin.email(), admin.password()));
            log.info("Admin user created: {}", admin.email());
        } catch (DuplicateEmailException e) {
            log.info("Admin user already exists, skipping: {}", admin.email());
        }

        DEFAULT_STATUSES.forEach(this::createIfMissing);
    }

    private void createIfMissing(TaskStatusCreateDTO dto) {
        if (taskStatusRepository.existsBySlug(dto.slug())) {
            log.info("TaskStatus already exists, skipping: {}", dto.slug());
            return;
        }
        taskStatusService.create(dto);
        log.info("TaskStatus created: {}", dto.slug());
    }
}
