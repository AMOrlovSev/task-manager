package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.dto.task.TaskFilterDTO;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.TaskMapperImpl;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskStatusRepository taskStatusRepository;
    @Mock private UserRepository userRepository;
    @Mock private LabelRepository labelRepository;

    private TaskMapperImpl mapper;
    private JsonMapper jsonMapper;
    private PatchSupport patchSupport;
    private TaskSpecification specification;
    private TaskService service;

    @BeforeEach
    void setUp() {
        mapper = new TaskMapperImpl();
        jsonMapper =
                JsonMapper.builder()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .build();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        patchSupport = new PatchSupport(jsonMapper, validator);
        specification = new TaskSpecification();
        service =
                new TaskService(
                        taskRepository,
                        taskStatusRepository,
                        userRepository,
                        labelRepository,
                        mapper,
                        patchSupport,
                        specification);
    }

    // ============================ findAll ============================

    @Test
    void findAllReturnsListOfDtos() {
        var status = existingStatus(1L, "Draft", "draft");
        var t1 = existingTask(1L, "Task1", status, null);
        var t2 = existingTask(2L, "Task2", status, null);
        when(taskRepository.findAll(any(Specification.class), eq(Sort.by("id"))))
                .thenReturn(List.of(t1, t2));

        var result = service.findAll(new TaskFilterDTO(null, null, null, null));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskDTO::title).containsExactly("Task1", "Task2");
        assertThat(result).extracting(TaskDTO::status).containsOnly("draft");
    }

    @Test
    void findAllReturnsEmptyList() {
        when(taskRepository.findAll(any(Specification.class), eq(Sort.by("id"))))
                .thenReturn(List.of());

        assertThat(service.findAll(new TaskFilterDTO(null, null, null, null))).isEmpty();
    }

    // ============================ findById ============================

    @Test
    void findByIdReturnsDto() {
        var status = existingStatus(1L, "Draft", "draft");
        var user = existingUser(2L, "u@x.com");
        var task = existingTask(1L, "Task", status, user);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        var dto = service.findById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Task");
        assertThat(dto.status()).isEqualTo("draft");
        assertThat(dto.assigneeId()).isEqualTo(2L);
        assertThat(dto.taskLabelIds()).isEmpty();
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ============================ create ============================

    @Test
    void createResolvesStatusAndSaves() {
        var status = existingStatus(1L, "Draft", "draft");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new TaskCreateDTO("Task", "Content", 0, "draft", null, null);
        var result = service.create(dto);

        assertThat(result.title()).isEqualTo("Task");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.index()).isZero();
        assertThat(result.status()).isEqualTo("draft");
        assertThat(result.assigneeId()).isNull();
        assertThat(result.taskLabelIds()).isEmpty();

        var saved = captureSaved();
        assertThat(saved.getName()).isEqualTo("Task");
        assertThat(saved.getDescription()).isEqualTo("Content");
        assertThat(saved.getIndex()).isZero();
        assertThat(saved.getTaskStatus()).isEqualTo(status);
        assertThat(saved.getAssignee()).isNull();
        assertThat(saved.getLabels()).isEmpty();
    }

    @Test
    void createResolvesAssigneeAndSaves() {
        var status = existingStatus(1L, "Draft", "draft");
        var user = existingUser(2L, "u@x.com");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new TaskCreateDTO("Task", null, null, "draft", 2L, null);
        service.create(dto);

        assertThat(captureSaved().getAssignee()).isEqualTo(user);
    }

    @Test
    void createWithoutAssigneeDoesNotCallUserRepository() {
        var status = existingStatus(1L, "Draft", "draft");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new TaskCreateDTO("Task", null, null, "draft", null, null);
        service.create(dto);

        verify(userRepository, never()).findById(any());
        assertThat(captureSaved().getAssignee()).isNull();
    }

    @Test
    void createTrimsTitle() {
        var status = existingStatus(1L, "Draft", "draft");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new TaskCreateDTO("  Padded  ", null, null, "draft", null, null);
        service.create(dto);

        assertThat(captureSaved().getName()).isEqualTo("Padded");
    }

    @Test
    void createWithUnknownStatusThrows() {
        when(taskStatusRepository.findBySlug("nope")).thenReturn(Optional.empty());

        var dto = new TaskCreateDTO("Task", null, null, "nope", null, null);
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("status"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createWithUnknownAssigneeThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        var dto = new TaskCreateDTO("Task", null, null, "draft", 999L, null);
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("assignee_id"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ create — labels ============================

    @Test
    void createWithLabelsResolvesAndSaves() {
        var status = existingStatus(1L, "Draft", "draft");
        var l1 = existingLabel(10L, "bug");
        var l2 = existingLabel(20L, "feature");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(labelRepository.findAllById(List.of(20L, 10L))).thenReturn(List.of(l1, l2));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new TaskCreateDTO("Task", null, null, "draft", null, List.of(20L, 10L));
        var result = service.create(dto);

        assertThat(result.taskLabelIds()).containsExactly(10L, 20L);
        assertThat(captureSaved().getLabels()).containsExactlyInAnyOrder(l1, l2);
    }

    @Test
    void createWithEmptyLabelListSkipsRepository() {
        var status = existingStatus(1L, "Draft", "draft");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new TaskCreateDTO("Task", null, null, "draft", null, List.of());
        service.create(dto);

        verify(labelRepository, never()).findAllById(any());
        assertThat(captureSaved().getLabels()).isEmpty();
    }

    @Test
    void createWithUnknownLabelThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        when(taskStatusRepository.findBySlug("draft")).thenReturn(Optional.of(status));
        when(labelRepository.findAllById(List.of(999L))).thenReturn(List.of());

        var dto = new TaskCreateDTO("Task", null, null, "draft", null, List.of(999L));
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("taskLabelIds"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ update — guard (400 раньше 404) ============================

    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"text\"", "null", "42"})
    void updateRejectsNonObjectBodyBeforeLookup(String body) {
        assertThatThrownBy(() -> service.update(999L, patch(body)))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("body"));

        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateRejectsJavaNullBodyBeforeLookup() {
        assertThatThrownBy(() -> service.update(999L, null))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("body"));

        verify(taskRepository, never()).findById(any());
    }

    @Test
    void updateWithNestedObjectInTitleThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"title": {"a": 1}}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("body"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ update — validation ============================

    @Test
    void updateWithNullTitleThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"title": null}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("title"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateWithBlankTitleThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"title": "   "}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("title"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateWithNullStatusThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"status": null}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("status"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateWithTooLongContentThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        var content = "a".repeat(4001);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
                                                """
        {"content": "%s"}"""
                                                        .formatted(content))))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("content"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ update — success ============================

    @Test
    void updateEmptyPatchKeepsEverything() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(1L, patch("{}"));

        assertThat(result.title()).isEqualTo("Task");
        assertThat(result.status()).isEqualTo("draft");
        assertThat(result.assigneeId()).isNull();
        assertThat(result.taskLabelIds()).isEmpty();
        verify(taskStatusRepository, never()).findBySlug(any());
        verify(userRepository, never()).findById(any());
        verify(labelRepository, never()).findAllById(any());
    }

    @Test
    void updateOnlyTitleTrims() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"title": "  New  "}"""));

        assertThat(result.title()).isEqualTo("New");
        assertThat(result.status()).isEqualTo("draft");
        verify(taskStatusRepository, never()).findBySlug(any());
    }

    @Test
    void updateContentToNullClears() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        task.setDescription("old content");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"content": null}"""));

        assertThat(result.content()).isNull();
        assertThat(captureSaved().getDescription()).isNull();
    }

    @Test
    void updateIndexOnly() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"index": 7}"""));

        assertThat(result.index()).isEqualTo(7);
    }

    @Test
    void updateIndexToNullClears() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        task.setIndex(5);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"index": null}"""));

        assertThat(result.index()).isNull();
    }

    @Test
    void updateIgnoresUnknownFields() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        // labels и assigneeId (camelCase) — не поля TaskUpdateDTO, должны быть проигнорированы
        var result =
                service.update(
                        1L,
                        patch(
                                """
                                {"title": "X", "labels": [1, 2], "assigneeId": 99}"""));

        assertThat(result.title()).isEqualTo("X");
        assertThat(result.assigneeId()).isNull();
        assertThat(result.taskLabelIds()).isEmpty();
        verify(userRepository, never()).findById(any());
        verify(labelRepository, never()).findAllById(any());
    }

    // ============================ update — status ============================

    @Test
    void updateStatusToDifferentSlugResolvesStatus() {
        var draft = existingStatus(1L, "Draft", "draft");
        var toReview = existingStatus(2L, "ToReview", "to_review");
        var task = existingTask(1L, "Task", draft, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskStatusRepository.findBySlug("to_review")).thenReturn(Optional.of(toReview));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"status": "to_review"}"""));

        assertThat(result.status()).isEqualTo("to_review");
        assertThat(task.getTaskStatus()).isEqualTo(toReview);
        verify(taskStatusRepository).findBySlug("to_review");
    }

    @Test
    void updateStatusToSameSlugDoesNotCallRepository() {
        var draft = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", draft, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(
                1L,
                patch(
                        """
                {"status": "draft"}"""));

        verify(taskStatusRepository, never()).findBySlug(any());
        assertThat(task.getTaskStatus()).isEqualTo(draft);
    }

    @Test
    void updateStatusToUnknownThrows() {
        var draft = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", draft, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskStatusRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"status": "nope"}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("status"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ update — assignee ============================

    @Test
    void updateAssigneeToDifferentIdResolvesUser() {
        var status = existingStatus(1L, "Draft", "draft");
        var oldUser = existingUser(2L, "old@x.com");
        var newUser = existingUser(3L, "new@x.com");
        var task = existingTask(1L, "Task", status, oldUser);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(3L)).thenReturn(Optional.of(newUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"assignee_id": 3}"""));

        assertThat(result.assigneeId()).isEqualTo(3L);
        assertThat(task.getAssignee()).isEqualTo(newUser);
        verify(userRepository).findById(3L);
    }

    @Test
    void updateAssigneeToNullClearsWithoutLookup() {
        var status = existingStatus(1L, "Draft", "draft");
        var user = existingUser(2L, "u@x.com");
        var task = existingTask(1L, "Task", status, user);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(
                1L,
                patch(
                        """
                {"assignee_id": null}"""));

        assertThat(task.getAssignee()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateAssigneeToSameIdDoesNotCallRepository() {
        var status = existingStatus(1L, "Draft", "draft");
        var user = existingUser(2L, "u@x.com");
        var task = existingTask(1L, "Task", status, user);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(
                1L,
                patch(
                        """
                {"assignee_id": 2}"""));

        verify(userRepository, never()).findById(any());
        assertThat(task.getAssignee()).isEqualTo(user);
    }

    @Test
    void updateAssigneeToUnknownThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var user = existingUser(2L, "u@x.com");
        var task = existingTask(1L, "Task", status, user);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"assignee_id": 999}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("assignee_id"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ update — labels ============================

    @Test
    void updateWithoutTaskLabelIdsKeyKeepsLabels() {
        var status = existingStatus(1L, "Draft", "draft");
        var label = existingLabel(5L, "bug");
        var task = existingTask(1L, "Task", status, null);
        task.getLabels().add(label);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"index": 42}"""));

        assertThat(result.taskLabelIds()).containsExactly(5L);
        verify(labelRepository, never()).findAllById(any());
        assertThat(task.getLabels()).containsExactly(label);
    }

    @Test
    void updateWithEmptyArrayClearsLabels() {
        var status = existingStatus(1L, "Draft", "draft");
        var label = existingLabel(5L, "bug");
        var task = existingTask(1L, "Task", status, null);
        task.getLabels().add(label);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"taskLabelIds": []}"""));

        assertThat(result.taskLabelIds()).isEmpty();
        assertThat(task.getLabels()).isEmpty();
        verify(labelRepository, never()).findAllById(any());
    }

    @Test
    void updateWithNullClearsLabels() {
        var status = existingStatus(1L, "Draft", "draft");
        var label = existingLabel(5L, "bug");
        var task = existingTask(1L, "Task", status, null);
        task.getLabels().add(label);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"taskLabelIds": null}"""));

        assertThat(result.taskLabelIds()).isEmpty();
        assertThat(task.getLabels()).isEmpty();
        verify(labelRepository, never()).findAllById(any());
    }

    @Test
    void updateReplacesLabels() {
        var status = existingStatus(1L, "Draft", "draft");
        var oldLabel = existingLabel(5L, "bug");
        var newLabel = existingLabel(7L, "feature");
        var task = existingTask(1L, "Task", status, null);
        task.getLabels().add(oldLabel);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(labelRepository.findAllById(List.of(7L))).thenReturn(List.of(newLabel));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"taskLabelIds": [7]}"""));

        assertThat(result.taskLabelIds()).containsExactly(7L);
        assertThat(task.getLabels()).containsExactly(newLabel);
    }

    @Test
    void updateWithSameLabelSetSkipsLookup() {
        var status = existingStatus(1L, "Draft", "draft");
        var label = existingLabel(5L, "bug");
        var task = existingTask(1L, "Task", status, null);
        task.getLabels().add(label);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                {"taskLabelIds": [5]}"""));

        assertThat(result.taskLabelIds()).containsExactly(5L);
        verify(labelRepository, never()).findAllById(any());
    }

    @Test
    void updateWithUnknownLabelThrows() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(labelRepository.findAllById(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
"""
{"taskLabelIds": [999]}""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("taskLabelIds"));

        verify(taskRepository, never()).save(any());
    }

    // ============================ misc ============================

    @Test
    void updateNonExistentTaskThrows() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        999L,
                                        patch(
"""
{"title": "X"}""")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void deleteExistingCallsDelete() {
        var status = existingStatus(1L, "Draft", "draft");
        var task = existingTask(1L, "Task", status, null);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        service.delete(1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteNonExistentThrows() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any(Task.class));
        verify(taskRepository, never()).deleteById(any());
    }

    // ============================ helpers ============================

    private TaskStatus existingStatus(Long id, String name, String slug) {
        var s = new TaskStatus();
        s.setId(id);
        s.setName(name);
        s.setSlug(slug);
        return s;
    }

    private User existingUser(Long id, String email) {
        var u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Label existingLabel(Long id, String name) {
        var l = new Label();
        l.setId(id);
        l.setName(name);
        return l;
    }

    private Task existingTask(Long id, String name, TaskStatus status, User assignee) {
        var t = new Task();
        t.setId(id);
        t.setName(name);
        t.setTaskStatus(status);
        t.setAssignee(assignee);
        t.setLabels(new HashSet<>());
        return t;
    }

    private JsonNode patch(String json) {
        return jsonMapper.readTree(json);
    }

    private Task captureSaved() {
        var captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unused")
    private Set<Label> labels(Label... labels) {
        return new HashSet<>(List.of(labels));
    }
}
