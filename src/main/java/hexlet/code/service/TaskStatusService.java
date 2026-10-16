package hexlet.code.service;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusDTO;
import hexlet.code.dto.task_status.TaskStatusUpdateDTO;
import hexlet.code.exception.DuplicateResourceException;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.mapper.TaskStatusMapper;
import hexlet.code.model.TaskStatus;
import hexlet.code.repository.TaskStatusRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
public class TaskStatusService {

    private final TaskStatusRepository repository;
    private final TaskStatusMapper mapper;
    private final PatchSupport patchSupport;

    public TaskStatusService(
            TaskStatusRepository repository, TaskStatusMapper mapper, PatchSupport patchSupport) {
        this.repository = repository;
        this.mapper = mapper;
        this.patchSupport = patchSupport;
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
