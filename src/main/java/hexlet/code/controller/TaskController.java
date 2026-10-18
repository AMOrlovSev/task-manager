package hexlet.code.controller;

import hexlet.code.dto.task.TaskCreateDTO;
import hexlet.code.dto.task.TaskDTO;
import hexlet.code.dto.task.TaskFilterDTO;
import hexlet.code.dto.task.TaskUpdateDTO;
import hexlet.code.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@Tag(name = "Tasks", description = "Задачи")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "Список задач с фильтрами")
    @ApiResponse(
            responseCode = "200",
            headers = @Header(name = "X-Total-Count", description = "Всего записей"))
    @GetMapping
    public ResponseEntity<List<TaskDTO>> index(@ParameterObject TaskFilterDTO filter) {
        var list = service.findAll(filter);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(list.size())).body(list);
    }

    @Operation(summary = "Задача по id")
    @GetMapping("/{id}")
    public TaskDTO show(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Создание задачи")
    @ApiResponse(responseCode = "201", description = "Задача создана")
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PostMapping
    public ResponseEntity<TaskDTO> create(@Valid @RequestBody TaskCreateDTO dto) {
        var created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @Operation(
            summary = "Частичное обновление задачи",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(implementation = TaskUpdateDTO.class))))
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskDTO update(@PathVariable Long id, @RequestBody JsonNode patchNode) {
        return service.update(id, patchNode);
    }

    @Operation(summary = "Удаление задачи")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
