package hexlet.code.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hexlet.code.dto.common.ListParams;
import hexlet.code.dto.common.SortField;
import hexlet.code.exception.ValidationException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PageableFactoryTest {

    private final PageableFactory factory = new PageableFactory(100);
    private final Map<String, SortField> allowed =
            Map.of(
                    "id", SortField.of("id"),
                    "title", SortField.text("name"),
                    "createdAt", SortField.of("createdAt"));

    @Test
    void emptyStartAndEndReturnsUnpaged() {
        var params = new ListParams(null, null, null, null);
        var pageable = factory.create(params, allowed);

        assertThat(pageable.isUnpaged()).isTrue();
        assertThat(pageable.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void onlyStartOrOnlyEndThrows() {
        var p1 = new ListParams(0, null, null, null);
        var p2 = new ListParams(null, 10, null, null);

        assertThatThrownBy(() -> factory.create(p1, allowed))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> factory.create(p2, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void negativeStartThrows() {
        var p = new ListParams(-5, 10, null, null);
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void endLessThanOrEqualStartThrows() {
        var p = new ListParams(10, 10, null, null);
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void sizeExceedsMaxThrows() {
        var p = new ListParams(0, 101, null, null);
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nonAlignedStartThrows() {
        var p = new ListParams(5, 15, null, null); // size = 10, 5 % 10 != 0
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validPageRequest() {
        var p = new ListParams(20, 30, null, null);
        var pageable = factory.create(p, allowed);

        assertThat(pageable.isPaged()).isTrue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void unknownSortFieldThrows() {
        var p = new ListParams(0, 10, "unknown", null);
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void lowercaseDescWorks() {
        var p = new ListParams(0, 10, "createdAt", "desc");
        var pageable = factory.create(p, allowed);

        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }

    @Test
    void invalidOrderThrows() {
        var p = new ListParams(0, 10, "id", "up");
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void secondSortKeyIsId() {
        var p = new ListParams(0, 10, "title", null);
        var pageable = factory.create(p, allowed);

        assertThat(pageable.getSort())
                .isEqualTo(
                        Sort.by(
                                Sort.Order.asc("name").ignoreCase().nullsLast(),
                                Sort.Order.asc("id") //
                                ));
    }

    @Test
    void noDuplicateIdSort() {
        var p = new ListParams(0, 10, "id", null);
        var pageable = factory.create(p, allowed);

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Order.asc("id").nullsLast()));
    }

    @Test
    void ignoreCaseIsSetForTextFields() {
        var p = new ListParams(0, 10, "title", null);
        var pageable = factory.create(p, allowed);

        assertThat(pageable.getSort().getOrderFor("name").isIgnoreCase()).isTrue();
    }

    @Test
    void exactlyMaxSizePasses() {
        var p = new ListParams(0, 100, null, null);
        var pageable = factory.create(p, allowed);
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void maxSizePlusOneThrows() {
        var p = new ListParams(0, 101, null, null);
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void hugeNumbersDoNotOverflow() {
        var p = new ListParams(0, Integer.MAX_VALUE, null, null);
        // 0 до Integer.MAX_VALUE = 2,147,483,647, что больше 100, поэтому должно выбросить ошибку
        assertThatThrownBy(() -> factory.create(p, allowed))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Validation failed");
    }
}
