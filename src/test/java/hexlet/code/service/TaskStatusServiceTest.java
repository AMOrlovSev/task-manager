package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusDTO;
import hexlet.code.exception.DuplicateResourceException;
import hexlet.code.exception.ResourceInUseException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.TaskStatusMapperImpl;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TaskStatusServiceTest {

    @Mock private TaskStatusRepository repository;
    @Mock private TaskRepository taskRepository;

    private TaskStatusMapperImpl mapper;
    private JsonMapper jsonMapper;
    private Validator validator;
    private TaskStatusService service;

    @BeforeEach
    void setUp() {
        mapper = new TaskStatusMapperImpl();
        jsonMapper =
                JsonMapper.builder()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .build();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        service =
                new TaskStatusService(
                        repository,
                        mapper,
                        new PatchSupport(jsonMapper, validator),
                        taskRepository);
    }

    // ============================ findAll / findById ============================

    @Test
    void findAllReturnsList() {
        when(repository.findAll(Sort.by("id")))
                .thenReturn(
                        List.of(
                                existing(1L, "Draft", "draft"),
                                existing(2L, "ToReview", "to_review")));

        var result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskStatusDTO::slug).containsExactly("draft", "to_review");
    }

    @Test
    void findByIdReturnsDto() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, "Draft", "draft")));

        var dto = service.findById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Draft");
        assertThat(dto.slug()).isEqualTo("draft");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ============================ create ============================

    @Test
    void createSavesEntity() {
        var dto = new TaskStatusCreateDTO("New", "new");
        when(repository.existsBySlug("new")).thenReturn(false);
        when(repository.existsByName("New")).thenReturn(false);
        when(repository.save(any(TaskStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(dto);

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.slug()).isEqualTo("new");
    }

    @Test
    void createThrowsWhenSlugExists() {
        var dto = new TaskStatusCreateDTO("New", "new");
        when(repository.existsBySlug("new")).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("new");

        verify(repository, never()).save(any());
    }

    @Test
    void createThrowsWhenNameExists() {
        var dto = new TaskStatusCreateDTO("New", "new");
        when(repository.existsBySlug("new")).thenReturn(false);
        when(repository.existsByName("New")).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("New");

        verify(repository, never()).save(any());
    }

    // ============================ update ============================

    @Test
    void updateRenamesName() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByName("Renamed")).thenReturn(false);
        when(repository.save(any(TaskStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                { "name": "Renamed" }"""));

        assertThat(result.name()).isEqualTo("Renamed");
        assertThat(result.slug()).isEqualTo("draft");
    }

    @Test
    void updateEmptyBodyKeepsEverything() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(TaskStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(1L, patch("{}"));

        assertThat(result.name()).isEqualTo("Draft");
        assertThat(result.slug()).isEqualTo("draft");
    }

    @Test
    void updateBlankNameFails() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
                                                """
                { "name": "" }""")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("name"));

        verify(repository, never()).save(any());
    }

    @Test
    void updateToDuplicateSlugFails() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsBySlug("taken")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
                                                """
                { "slug": "taken" }""")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateSameSlugIsNoop() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(TaskStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
                { "slug": "draft" }"""));

        assertThat(result.slug()).isEqualTo("draft");
        verify(repository, never()).existsBySlug(any());
    }

    @Test
    void updateNonExistentThrows() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.update(
                                        999L,
                                        patch(
                                                """
                { "name": "X" }""")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateArrayBodyFails() {
        assertThatThrownBy(() -> service.update(1L, patch("[1,2,3]")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("body"));

        verify(repository, never()).save(any());
    }

    @Test
    void updateStringBodyFails() {
        assertThatThrownBy(() -> service.update(1L, patch("\"x\"")))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).save(any());
    }

    // ============================ delete ============================

    @Test
    void deleteExisting() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(taskRepository.existsByTaskStatusId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void deleteNonExistentThrows() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteStatusUsedByTaskThrows() {
        var entity = existing(1L, "Draft", "draft");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(taskRepository.existsByTaskStatusId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("used by tasks");

        verify(repository, never()).delete(any());
    }

    // ============================ helpers ============================

    private TaskStatus existing(Long id, String name, String slug) {
        var status = new TaskStatus();
        status.setId(id);
        status.setName(name);
        status.setSlug(slug);
        return status;
    }

    private JsonNode patch(String json) {
        return jsonMapper.readTree(json);
    }
}
