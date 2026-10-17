package hexlet.code.service;

import hexlet.code.component.PatchSupport;
import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.exception.ResourceNotFoundException;
import hexlet.code.exception.ValidationException;
import hexlet.code.mapper.TaskMapper;
import hexlet.code.model.Label;
import hexlet.code.model.Task;
import hexlet.code.model.TaskStatus;
import hexlet.code.model.User;
import hexlet.code.repository.LabelRepository;
import hexlet.code.repository.TaskRepository;
import hexlet.code.repository.TaskStatusRepository;
import hexlet.code.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final LabelRepository labelRepository;
    private final TaskMapper mapper;
    private final PatchSupport patchSupport;

    public TaskService(
            TaskRepository taskRepository,
            TaskStatusRepository taskStatusRepository,
            UserRepository userRepository, LabelRepository labelRepository,
            TaskMapper mapper,
            PatchSupport patchSupport) {
        this.taskRepository = taskRepository;
        this.taskStatusRepository = taskStatusRepository;
        this.userRepository = userRepository;
        this.labelRepository = labelRepository;
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
        task.setLabels(resolveLabels(dto.taskLabelIds()));
        return mapper.toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDTO update(Long id, JsonNode patchNode) {
        patchSupport.requireObject(patchNode);
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

        applyLabelsPatch(task, dto.getTaskLabelIds(), patchNode.has("taskLabelIds"));

        mapper.update(dto, task);
        return mapper.toDto(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(getOrThrow(id));
    }

    private void applyLabelsPatch(Task task, List<Long> requestedIds, boolean keyPresent) {
        if (!keyPresent) {
            return;
        }
        if (requestedIds == null) {
            task.setLabels(new HashSet<>());
            return;
        }
        Set<Long> requested = new HashSet<>(requestedIds);
        Set<Long> current =
                task.getLabels().stream().map(Label::getId).collect(Collectors.toSet());
        if (!requested.equals(current)) {
            task.setLabels(resolveLabels(requestedIds));
        }
    }

    private Set<Label> resolveLabels(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        var distinct = ids.stream().distinct().toList();
        var labels = labelRepository.findAllById(distinct);
        if (labels.size() < distinct.size()) {
            var foundIds = labels.stream().map(Label::getId).collect(Collectors.toSet());
            var missing = distinct.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ValidationException(
                    Map.of("taskLabelIds", "Labels not found: " + missing));
        }
        return new HashSet<>(labels);
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
