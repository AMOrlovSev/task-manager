package hexlet.code.service;

import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusDTO;
import hexlet.code.dto.task_status.TaskStatusUpdateDTO;
import hexlet.code.exception.DuplicateNameException;
import hexlet.code.exception.DuplicateSlugException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.TaskStatusMapper;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskStatusRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
public class TaskStatusService {

    private final TaskStatusRepository repository;
    private final TaskStatusMapper mapper;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public TaskStatusService(
            TaskStatusRepository repository,
            TaskStatusMapper mapper,
            JsonMapper jsonMapper,
            Validator validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public List<TaskStatusDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public TaskStatusDTO findById(Long id) {
        return mapper.toDto(getOrThrow(id));
    }

    @Transactional
    public TaskStatusDTO create(TaskStatusCreateDTO dto) {
        if (repository.existsBySlug(dto.slug())) {
            throw new DuplicateSlugException(
                    "TaskStatus with slug " + dto.slug() + " already exists");
        }
        if (repository.existsByName(dto.name())) {
            throw new DuplicateNameException(
                    "TaskStatus with name " + dto.name() + " already exists");
        }
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Transactional
    public TaskStatusDTO update(Long id, JsonNode patchNode) {
        if (patchNode == null || !patchNode.isObject()) {
            throw new ValidationException(Map.of("body", "Patch body must be a JSON object"));
        }
        var entity = getOrThrow(id);

        var currentJson = jsonMapper.valueToTree(mapper.toUpdateDto(entity));
        var patchedJson = jsonMapper.updateValue(currentJson, patchNode);
        var dto = jsonMapper.treeToValue(patchedJson, TaskStatusUpdateDTO.class);

        Set<ConstraintViolation<TaskStatusUpdateDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (var v : violations) {
                errors.put(v.getPropertyPath().toString(), v.getMessage());
            }
            throw new ValidationException(errors);
        }

        if (dto.getSlug() != null
                && !dto.getSlug().equals(entity.getSlug())
                && repository.existsBySlug(dto.getSlug())) {
            throw new DuplicateSlugException(
                    "TaskStatus with slug " + dto.getSlug() + " already exists");
        }
        if (dto.getName() != null
                && !dto.getName().equals(entity.getName())
                && repository.existsByName(dto.getName())) {
            throw new DuplicateNameException(
                    "TaskStatus with name " + dto.getName() + " already exists");
        }

        mapper.update(dto, entity);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private TaskStatus getOrThrow(Long id) {
        return repository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "TaskStatus with id " + id + " not found"));
    }
}