package hexlet.code.repository;

import hexlet.code.model.TaskStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusRepository extends JpaRepository<TaskStatus, Long> {
    Optional<TaskStatus> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
