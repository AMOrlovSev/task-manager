package hexlet.code.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import java.util.List;
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
        var dto = new TaskCreateDTO("Title", "Content", 4, "draft", 9L, null);
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

    @Test
    void toDtoMapsLabelsToSortedIds() {
        var l1 = new Label();
        l1.setId(10L);
        var l2 = new Label();
        l2.setId(2L);
        var s = new TaskStatus();
        s.setSlug("draft");
        var t = new Task();
        t.setTaskStatus(s);
        t.getLabels().add(l1);
        t.getLabels().add(l2);

        var dto = mapper.toDto(t);

        assertThat(dto.taskLabelIds()).containsExactly(2L, 10L);
    }

    @Test
    void toDtoWithNoLabelsReturnsEmptyList() {
        var s = new TaskStatus();
        s.setSlug("draft");
        var t = new Task();
        t.setTaskStatus(s);

        assertThat(mapper.toDto(t).taskLabelIds()).isEmpty();
    }

    @Test
    void toUpdateDtoMapsLabelsToSortedIds() {
        var l1 = new Label();
        l1.setId(30L);
        var l2 = new Label();
        l2.setId(1L);
        var s = new TaskStatus();
        s.setSlug("draft");
        var t = new Task();
        t.setTaskStatus(s);
        t.getLabels().add(l1);
        t.getLabels().add(l2);

        var dto = mapper.toUpdateDto(t);

        assertThat(dto.getTaskLabelIds()).containsExactly(1L, 30L);
    }

    @Test
    void updateDoesNotTouchLabels() {
        var t = new Task();
        var label = new Label();
        label.setId(5L);
        t.getLabels().add(label);

        var dto = new TaskUpdateDTO();
        dto.setTitle("New");
        dto.setTaskLabelIds(List.of(999L));

        mapper.update(dto, t);

        assertThat(t.getLabels()).containsExactly(label);
    }
}
