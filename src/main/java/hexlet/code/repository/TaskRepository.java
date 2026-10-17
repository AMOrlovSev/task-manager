package hexlet.code.repository;

import hexlet.code.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByAssigneeId(Long assigneeId);

    boolean existsByTaskStatusId(Long statusId);
}
