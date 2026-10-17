package hexlet.code.component;

import hexlet.code.config.AdminProperties;
import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.exception.DuplicateEmailException;
import hexlet.code.service.LabelService;
import hexlet.code.service.TaskStatusService;
import hexlet.code.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminProperties admin;
    private final UserService userService;
    private final TaskStatusService taskStatusService;
    private final LabelService labelService;

    public DataInitializer(
            AdminProperties admin,
            UserService userService,
            TaskStatusService taskStatusService,
            LabelService labelService) {
        this.admin = admin;
        this.userService = userService;
        this.taskStatusService = taskStatusService;
        this.labelService = labelService;
    }

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfMissing();
        taskStatusService.createDefaultsIfMissing();
        labelService.createDefaultsIfMissing();
    }

    private void createAdminIfMissing() {
        try {
            userService.create(new UserCreateDTO(null, null, admin.email(), admin.password()));
            log.info("Admin user created: {}", admin.email());
        } catch (DuplicateEmailException e) {
            log.info("Admin user already exists, skipping: {}", admin.email());
        }
    }
}
