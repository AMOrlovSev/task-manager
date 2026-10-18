package hexlet.code.service;

import hexlet.code.dto.task.TaskFilterDTO;
import hexlet.code.model.Task;
import jakarta.persistence.criteria.Expression;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class TaskSpecification {

    public Specification<Task> build(TaskFilterDTO filter) {
        if (filter == null) {
            filter = new TaskFilterDTO(null, null, null, null);
        }
        return withTitleCont(filter.titleCont())
                .and(withAssigneeId(filter.assigneeId()))
                .and(withStatus(filter.status()))
                .and(withLabelId(filter.labelId()));
    }

    private Specification<Task> withTitleCont(String value) {
        if (value == null || value.isBlank()) {
            return Specification.unrestricted();
        }
        var pattern = "%" + escapeLike(value.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Expression<String> lowerName = cb.lower(root.get("name"));
            return cb.like(lowerName, pattern, '\\');
        };
    }

    private Specification<Task> withAssigneeId(Long id) {
        if (id == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), id);
    }

    private Specification<Task> withStatus(String slug) {
        if (slug == null || slug.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.join("taskStatus").get("slug"), slug.strip());
    }

    private Specification<Task> withLabelId(Long id) {
        if (id == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var task = subquery.from(Task.class);
            var labels = task.join("labels");
            subquery.select(task.get("id"))
                    .where(
                            cb.equal(task.get("id"), root.get("id")),
                            cb.equal(labels.get("id"), id));
            return cb.exists(subquery);
        };
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
