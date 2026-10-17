package hexlet.code.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.dto.label.LabelCreateDTO;
import hexlet.code.dto.label.LabelUpdateDTO;
import hexlet.code.model.Label;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class LabelMapperTest {

    private final LabelMapper mapper = new LabelMapperImpl();

    // ============================ toDto ============================

    @Test
    void toDtoMapsAllFields() {
        var label = new Label();
        label.setId(5L);
        label.setName("feature");
        label.setCreatedAt(LocalDateTime.of(2026, 10, 9, 13, 7));

        var dto = mapper.toDto(label);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.name()).isEqualTo("feature");
        assertThat(dto.createdAt()).isEqualTo(java.time.LocalDate.of(2026, 10, 9));
    }

    @Test
    void toDtoWithNullCreatedAt() {
        var label = new Label();
        label.setName("feature");

        assertThat(mapper.toDto(label).createdAt()).isNull();
    }

    // ============================ toEntity ============================

    @Test
    void toEntityMapsFieldsAndIgnoresProtected() {
        var dto = new LabelCreateDTO("feature");

        var label = mapper.toEntity(dto);

        assertThat(label.getName()).isEqualTo("feature");
        assertThat(label.getId()).isNull();
        assertThat(label.getCreatedAt()).isNull();
        assertThat(label.getUpdatedAt()).isNull();
    }

    @Test
    void toEntityAppliesStripFromDto() {
        var dto = new LabelCreateDTO("  feature  ");

        var label = mapper.toEntity(dto);

        assertThat(label.getName()).isEqualTo("feature");
    }

    // ============================ update ============================

    @Test
    void updateReplacesName() {
        var label = new Label();
        label.setName("old");

        var dto = new LabelUpdateDTO();
        dto.setName("new");

        mapper.update(dto, label);

        assertThat(label.getName()).isEqualTo("new");
    }

    @Test
    void updateAppliesStripFromDto() {
        var label = new Label();
        label.setName("old");

        var dto = new LabelUpdateDTO();
        dto.setName("  new  ");

        mapper.update(dto, label);

        assertThat(label.getName()).isEqualTo("new");
    }

    @Test
    void updateWithNullNameOverwritesTarget() {
        var label = new Label();
        label.setName("feature");
        label.setId(1L);
        label.setCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));

        var dto = new LabelUpdateDTO();

        mapper.update(dto, label);

        assertThat(label.getName()).isNull();
        assertThat(label.getId()).isEqualTo(1L);
        assertThat(label.getCreatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    // ============================ toUpdateDto ============================

    @Test
    void toUpdateDtoMapsName() {
        var label = new Label();
        label.setName("feature");

        var dto = mapper.toUpdateDto(label);

        assertThat(dto.getName()).isEqualTo("feature");
    }
}
