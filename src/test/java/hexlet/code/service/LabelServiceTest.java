package hexlet.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.label.LabelCreateDTO;
import hexlet.code.dto.label.LabelDTO;
import hexlet.code.exception.DuplicateResourceException;
import hexlet.code.exception.ResourceInUseException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.LabelMapperImpl;
import hexlet.code.model.Label;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock private LabelRepository repository;
    @Mock private TaskRepository taskRepository;

    private LabelMapperImpl mapper;
    private JsonMapper jsonMapper;
    private Validator validator;
    private LabelService service;

    @BeforeEach
    void setUp() {
        mapper = new LabelMapperImpl();
        jsonMapper =
                JsonMapper.builder()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .build();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        service =
                new LabelService(
                        repository,
                        taskRepository,
                        mapper,
                        new PatchSupport(jsonMapper, validator));
    }

    // ============================ findAll / findById ============================

    @Test
    void findAllReturnsSortedList() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing(1L, "bug"), existing(2L, "feature"))));

        var result = service.findAll(Pageable.unpaged(Sort.by("id")));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(LabelDTO::name)
                .containsExactly("bug", "feature");
    }

    @Test
    void findByIdReturnsDto() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing(1L, "bug")));

        var dto = service.findById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("bug");
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
        var dto = new LabelCreateDTO("backend");
        when(repository.existsByName("backend")).thenReturn(false);
        when(repository.save(any(Label.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(dto);

        assertThat(result.name()).isEqualTo("backend");
    }

    @Test
    void createThrowsWhenNameExists() {
        var dto = new LabelCreateDTO("backend");
        when(repository.existsByName("backend")).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("backend");

        verify(repository, never()).save(any());
    }

    // ============================ update ============================

    @Test
    void updateRenamesName() {
        var entity = existing(1L, "old");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByName("renamed")).thenReturn(false);
        when(repository.save(any(Label.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
            { "name": "renamed" }"""));

        assertThat(result.name()).isEqualTo("renamed");
    }

    @Test
    void updateEmptyBodyKeepsEverything() {
        var entity = existing(1L, "old");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(Label.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(1L, patch("{}"));

        assertThat(result.name()).isEqualTo("old");
    }

    @Test
    void updateSameNameIsNoop() {
        var entity = existing(1L, "same");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(Label.class))).thenAnswer(inv -> inv.getArgument(0));

        var result =
                service.update(
                        1L,
                        patch(
                                """
            { "name": "same" }"""));

        assertThat(result.name()).isEqualTo("same");
        verify(repository, never()).existsByName(any());
    }

    @Test
    void updateToDuplicateNameFails() {
        var entity = existing(1L, "mine");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByName("taken")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        1L,
                                        patch(
                                                """
                { "name": "taken" }""")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateBlankNameFails() {
        var entity = existing(1L, "valid");
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
    void updateArrayBodyFails() {
        assertThatThrownBy(() -> service.update(1L, patch("[1,2,3]")))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("body"));

        verify(repository, never()).save(any());
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

    // ============================ delete ============================

    @Test
    void deleteExisting() {
        var entity = existing(1L, "to-delete");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(taskRepository.existsByLabelsId(1L)).thenReturn(false);

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
    void deleteUsedLabelThrows() {
        var entity = existing(1L, "in-use");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(taskRepository.existsByLabelsId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("used by tasks");

        verify(repository, never()).delete(any());
    }

    // ============================ helpers ============================

    private Label existing(Long id, String name) {
        var label = new Label();
        label.setId(id);
        label.setName(name);
        return label;
    }

    private JsonNode patch(String json) {
        return jsonMapper.readTree(json);
    }
}
