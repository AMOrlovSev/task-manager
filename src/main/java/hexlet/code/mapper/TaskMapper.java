package hexlet.code.mapper;

import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.model.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class TaskMapper {

    @Mapping(source = "name", target = "title")
    @Mapping(source = "description", target = "content")
    @Mapping(source = "taskStatus.slug", target = "status")
    @Mapping(source = "assignee.id", target = "assigneeId")
    public abstract TaskDTO toDto(Task task);

    @Mapping(source = "title", target = "name")
    @Mapping(source = "content", target = "description")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskStatus", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Task toEntity(TaskCreateDTO dto);

    @Mapping(source = "title", target = "name")
    @Mapping(source = "content", target = "description")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskStatus", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void update(TaskUpdateDTO dto, @MappingTarget Task task);

    @Mapping(source = "name", target = "title")
    @Mapping(source = "description", target = "content")
    @Mapping(source = "taskStatus.slug", target = "status")
    @Mapping(source = "assignee.id", target = "assigneeId")
    public abstract TaskUpdateDTO toUpdateDto(Task task);
}
