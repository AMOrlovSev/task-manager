package hexlet.code.dto.common;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

public final class PageResponses {
    private PageResponses() {}

    public static <T> ResponseEntity<List<T>> of(Page<T> page) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(page.getTotalElements()))
                .body(page.getContent());
    }
}
