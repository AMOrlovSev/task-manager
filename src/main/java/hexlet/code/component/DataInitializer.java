package hexlet.code.component;

import hexlet.code.config.AdminProperties;
import hexlet.code.dto.label.LabelCreateDTO;
import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.service.LabelService;
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

    private static final List<LabelCreateDTO> DEFAULT_LABELS =
            List.of(new LabelCreateDTO("feature"), new LabelCreateDTO("bug"));

    private final AdminProperties admin;
    private final UserService userService;
    private final TaskStatusService taskStatusService;
    private final TaskStatusRepository taskStatusRepository;
    private final LabelService labelService;
    private final LabelRepository labelRepository;

    public DataInitializer(
            AdminProperties admin,
            UserService userService,
            TaskStatusService taskStatusService,
            TaskStatusRepository taskStatusRepository,
            LabelService labelService,
            LabelRepository labelRepository) {
        this.admin = admin;
        this.userService = userService;
        this.taskStatusService = taskStatusService;
        this.taskStatusRepository = taskStatusRepository;
        this.labelService = labelService;
        this.labelRepository = labelRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfMissing();
        DEFAULT_STATUSES.forEach(this::createStatusIfMissing);
        DEFAULT_LABELS.forEach(this::createLabelIfMissing);
    }

    private void createAdminIfMissing() {
        try {
            userService.create(new UserCreateDTO(null, null, admin.email(), admin.password()));
            log.info("Admin user created: {}", admin.email());
        } catch (DuplicateEmailException e) {
            log.info("Admin user already exists, skipping: {}", admin.email());
        }
    }

    private void createStatusIfMissing(TaskStatusCreateDTO dto) {
        if (taskStatusRepository.existsBySlug(dto.slug())
                || taskStatusRepository.existsByName(dto.name())) {
            log.info("TaskStatus already exists, skipping: {}", dto.slug());
            return;
        }
        taskStatusService.create(dto);
        log.info("TaskStatus created: {}", dto.slug());
    }

    private void createLabelIfMissing(LabelCreateDTO dto) {
        if (labelRepository.existsByName(dto.name())) {
            log.info("Label already exists, skipping: {}", dto.name());
            return;
        }
        labelService.create(dto);
        log.info("Label created: {}", dto.name());
    }
}
