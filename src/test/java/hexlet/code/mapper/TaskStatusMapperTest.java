package hexlet.code.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusUpdateDTO;
import hexlet.code.model.TaskStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TaskStatusMapperTest {

    private final TaskStatusMapper mapper = new TaskStatusMapperImpl();

    @Test
    void toDtoMapsAllFields() {
        var entity = new TaskStatus();
        entity.setId(7L);
        entity.setName("Draft");
        entity.setSlug("draft");
        entity.setCreatedAt(LocalDateTime.of(2026, 10, 30, 13, 0));

        var dto = mapper.toDto(entity);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Draft");
        assertThat(dto.slug()).isEqualTo("draft");
        assertThat(dto.createdAt()).isEqualTo(java.time.LocalDate.of(2026, 10, 30));
    }

    @Test
    void toEntityMapsFieldsAndIgnoresProtected() {
        var dto = new TaskStatusCreateDTO("Draft", "draft");

        var entity = mapper.toEntity(dto);

        assertThat(entity.getName()).isEqualTo("Draft");
        assertThat(entity.getSlug()).isEqualTo("draft");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void updateReplacesBothFields() {
        var entity = new TaskStatus();
        entity.setName("Old");
        entity.setSlug("old");

        var dto = new TaskStatusUpdateDTO();
        dto.setName("New");
        dto.setSlug("new");

        mapper.update(dto, entity);

        assertThat(entity.getName()).isEqualTo("New");
        assertThat(entity.getSlug()).isEqualTo("new");
    }

    @Test
    void updateWithNullFieldsOverwritesTarget() {
        var entity = new TaskStatus();
        entity.setName("Draft");
        entity.setSlug("draft");

        var dto = new TaskStatusUpdateDTO();

        mapper.update(dto, entity);

        assertThat(entity.getName()).isNull();
        assertThat(entity.getSlug()).isNull();
    }

    @Test
    void toUpdateDtoMapsBothFields() {
        var entity = new TaskStatus();
        entity.setName("Draft");
        entity.setSlug("draft");

        var dto = mapper.toUpdateDto(entity);

        assertThat(dto.getName()).isEqualTo("Draft");
        assertThat(dto.getSlug()).isEqualTo("draft");
    }
}
