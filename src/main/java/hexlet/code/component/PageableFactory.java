package hexlet.code.component;

import hexlet.code.dto.common.ListParams;
import hexlet.code.dto.common.SortField;
import hexlet.code.exception.ValidationException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableFactory {

    private final int maxPageSize;

    public PageableFactory(@Value("${app.pagination.max-size:50}") int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public Pageable create(ListParams p, Map<String, SortField> allowed) {
        Sort sort = buildSort(p, allowed);

        if (p.start() == null && p.end() == null) {
            return Pageable.unpaged(sort);
        }
        if (p.start() == null || p.end() == null) {
            throw invalid("_start", "_start and _end must be used together");
        }

        long start = p.start();
        long size = (long) p.end() - start;

        if (start < 0) {
            throw invalid("_start", "_start must not be negative");
        }
        if (size <= 0) {
            throw invalid("_end", "_end must be greater than _start");
        }
        if (size > maxPageSize) {
            throw invalid("_end", "Page size must not exceed " + maxPageSize);
        }
        if (start % size != 0) {
            throw invalid("_start", "_start must be a multiple of page size (_end - _start)");
        }

        return PageRequest.of((int) (start / size), (int) size, sort);
    }

    private Sort buildSort(ListParams p, Map<String, SortField> allowed) {
        if (p.sort() == null || p.sort().isBlank()) {
            return Sort.by("id");
        }

        var field = allowed.get(p.sort().strip());
        if (field == null) {
            throw invalid(
                    "_sort", "Unknown sort field. Allowed: " + new TreeSet<>(allowed.keySet()));
        }

        var order = new Sort.Order(parseDirection(p.order()), field.property());
        if (field.ignoreCase()) {
            order = order.ignoreCase();
        }

        var sort = Sort.by(order);
        return "id".equals(field.property()) ? sort : sort.and(Sort.by("id"));
    }

    private Sort.Direction parseDirection(String order) {
        if (order == null || order.isBlank()) {
            return Sort.Direction.ASC;
        }
        return switch (order.strip().toUpperCase(Locale.ROOT)) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default -> throw invalid("_order", "_order must be ASC or DESC");
        };
    }

    private ValidationException invalid(String key, String message) {
        return new ValidationException(Map.of(key, message));
    }
}
