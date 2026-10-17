package hexlet.code.service;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.TaskMapper;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final UserRepository userRepository;
    private final TaskMapper mapper;
    private final PatchSupport patchSupport;

    public TaskService(
            TaskRepository taskRepository,
            TaskStatusRepository taskStatusRepository,
            UserRepository userRepository,
            TaskMapper mapper,
            PatchSupport patchSupport) {
        this.taskRepository = taskRepository;
        this.taskStatusRepository = taskStatusRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.patchSupport = patchSupport;
    }

    public List<TaskDTO> findAll() {
        return taskRepository.findAll(Sort.by("id")).stream().map(mapper::toDto).toList();
    }

    public TaskDTO findById(Long id) {
        return mapper.toDto(getOrThrow(id));
    }

    @Transactional
    public TaskDTO create(TaskCreateDTO dto) {
        var status = resolveStatus(dto.status().trim());
        var assignee = resolveAssignee(dto.assigneeId());

        var task = mapper.toEntity(dto);
        task.setName(dto.title().trim());
        task.setTaskStatus(status);
        task.setAssignee(assignee);
        return mapper.toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDTO update(Long id, JsonNode patchNode) {
        var task = getOrThrow(id);
        var dto = patchSupport.apply(mapper.toUpdateDto(task), patchNode, TaskUpdateDTO.class);

        dto.setTitle(dto.getTitle().trim());

        String newStatusSlug = dto.getStatus().trim();
        if (!newStatusSlug.equals(task.getTaskStatus().getSlug())) {
            task.setTaskStatus(resolveStatus(newStatusSlug));
        }

        Long currentAssigneeId = task.getAssignee() == null ? null : task.getAssignee().getId();
        if (!Objects.equals(dto.getAssigneeId(), currentAssigneeId)) {
            task.setAssignee(resolveAssignee(dto.getAssigneeId()));
        }

        mapper.update(dto, task);
        return mapper.toDto(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(getOrThrow(id));
    }

    private Task getOrThrow(Long id) {
        return taskRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    private TaskStatus resolveStatus(String slug) {
        return taskStatusRepository
                .findBySlug(slug)
                .orElseThrow(
                        () ->
                                new ValidationException(
                                        Map.of("status", "Task status '" + slug + "' not found")));
    }

    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository
                .findById(assigneeId)
                .orElseThrow(
                        () ->
                                new ValidationException(
                                        Map.of(
                                                "assignee_id",
                                                "User with id " + assigneeId + " not found")));
    }
}
