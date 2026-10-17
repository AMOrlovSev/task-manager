package hexlet.code.service;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusDTO;
import hexlet.code.dto.task_status.TaskStatusUpdateDTO;
import hexlet.code.exception.DuplicateResourceException;
import hexlet.code.exception.ResourceInUseException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.mapper.TaskStatusMapper;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
public class TaskStatusService {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusService.class);

    private static final List<TaskStatusCreateDTO> DEFAULT_STATUSES =
            List.of(
                    new TaskStatusCreateDTO("Draft", "draft"),
                    new TaskStatusCreateDTO("ToReview", "to_review"),
                    new TaskStatusCreateDTO("ToBeFixed", "to_be_fixed"),
                    new TaskStatusCreateDTO("ToPublish", "to_publish"),
                    new TaskStatusCreateDTO("Published", "published"));

    private final TaskStatusRepository repository;
    private final TaskStatusMapper mapper;
    private final PatchSupport patchSupport;
    private final TaskRepository taskRepository;

    public TaskStatusService(
            TaskStatusRepository repository,
            TaskStatusMapper mapper,
            PatchSupport patchSupport,
            TaskRepository taskRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.patchSupport = patchSupport;
        this.taskRepository = taskRepository;
    }

    public List<TaskStatusDTO> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::toDto).toList();
    }

    public TaskStatusDTO findById(Long id) {
        return mapper.toDto(getOrThrow(id));
    }

    @Transactional
    public TaskStatusDTO create(TaskStatusCreateDTO dto) {
        String name = dto.name().trim();
        String slug = dto.slug().trim();

        if (repository.existsBySlug(slug)) {
            throw new DuplicateResourceException(
                    "TaskStatus with slug " + slug + " already exists");
        }
        if (repository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "TaskStatus with name " + name + " already exists");
        }

        var entity = mapper.toEntity(dto);
        entity.setName(name);
        entity.setSlug(slug);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public TaskStatusDTO update(Long id, JsonNode patchNode) {
        patchSupport.requireObject(patchNode);
        var entity = getOrThrow(id);

        var dto =
                patchSupport.apply(
                        mapper.toUpdateDto(entity), patchNode, TaskStatusUpdateDTO.class);

        dto.setName(dto.getName().trim());
        dto.setSlug(dto.getSlug().trim());

        if (!dto.getSlug().equals(entity.getSlug()) && repository.existsBySlug(dto.getSlug())) {
            throw new DuplicateResourceException(
                    "TaskStatus with slug " + dto.getSlug() + " already exists");
        }
        if (!dto.getName().equals(entity.getName()) && repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException(
                    "TaskStatus with name " + dto.getName() + " already exists");
        }

        mapper.update(dto, entity);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        var status = getOrThrow(id);
        if (taskRepository.existsByTaskStatusId(id)) {
            throw new ResourceInUseException(
                    "TaskStatus with id " + id + " is used by tasks and cannot be deleted");
        }
        repository.delete(status);
    }

    private TaskStatus getOrThrow(Long id) {
        return repository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "TaskStatus with id " + id + " not found"));
    }

    @Transactional
    public void createDefaultsIfMissing() {
        for (TaskStatusCreateDTO dto : DEFAULT_STATUSES) {
            if (repository.existsBySlug(dto.slug()) || repository.existsByName(dto.name())) {
                log.info("TaskStatus already exists, skipping: {}", dto.slug());
                continue;
            }
            create(dto);
            log.info("TaskStatus created: {}", dto.slug());
        }
    }
}
