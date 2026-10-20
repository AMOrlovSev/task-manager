package hexlet.code.repository;

import hexlet.code.model.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Override
    @EntityGraph(attributePaths = {"taskStatus", "labels"})
    List<Task> findAll(Specification<Task> spec, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"taskStatus"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"taskStatus", "labels"})
    Optional<Task> findWithDetailsById(Long id);

    boolean existsByAssigneeId(Long assigneeId);

    boolean existsByTaskStatusId(Long statusId);

    boolean existsByLabelsId(Long id);
}
