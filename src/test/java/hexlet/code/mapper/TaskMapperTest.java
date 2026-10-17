package hexlet.code.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import org.junit.jupiter.api.Test;

class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapperImpl();

    @Test
    void toDtoMapsAllFields() {
        var u = new User();
        u.setId(9L);
        var s = new TaskStatus();
        s.setSlug("draft");
        var t = new Task();
        t.setId(1L);
        t.setName("N");
        t.setDescription("C");
        t.setIndex(3);
        t.setTaskStatus(s);
        t.setAssignee(u);

        var dto = mapper.toDto(t);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("N");
        assertThat(dto.content()).isEqualTo("C");
        assertThat(dto.index()).isEqualTo(3);
        assertThat(dto.status()).isEqualTo("draft");
        assertThat(dto.assigneeId()).isEqualTo(9L);
    }

    @Test
    void toDtoWithNullAssignee() {
        var s = new TaskStatus();
        s.setSlug("draft");
        var t = new Task();
        t.setName("N");
        t.setTaskStatus(s);

        var dto = mapper.toDto(t);

        assertThat(dto.assigneeId()).isNull();
    }

    @Test
    void toEntityMapsNameFields() {
        var dto = new TaskCreateDTO("Title", "Content", 4, "draft", 9L);
        var t = mapper.toEntity(dto);

        assertThat(t.getName()).isEqualTo("Title");
        assertThat(t.getDescription()).isEqualTo("Content");
        assertThat(t.getIndex()).isEqualTo(4);
        assertThat(t.getId()).isNull();
        assertThat(t.getTaskStatus()).isNull();
        assertThat(t.getAssignee()).isNull();
    }

    @Test
    void updateCopiesNameAndDescription() {
        var t = new Task();
        t.setName("Old");
        t.setDescription("OldC");
        var dto = new TaskUpdateDTO();
        dto.setTitle("New");
        dto.setContent("NewC");
        dto.setStatus("draft");
        dto.setIndex(7);

        mapper.update(dto, t);

        assertThat(t.getName()).isEqualTo("New");
        assertThat(t.getDescription()).isEqualTo("NewC");
        assertThat(t.getIndex()).isEqualTo(7);
    }

    @Test
    void toUpdateDtoMapsStatusAndAssignee() {
        var s = new TaskStatus();
        s.setSlug("draft");
        var u = new User();
        u.setId(5L);
        var t = new Task();
        t.setName("N");
        t.setTaskStatus(s);
        t.setAssignee(u);

        var dto = mapper.toUpdateDto(t);

        assertThat(dto.getTitle()).isEqualTo("N");
        assertThat(dto.getStatus()).isEqualTo("draft");
        assertThat(dto.getAssigneeId()).isEqualTo(5L);
    }
}
