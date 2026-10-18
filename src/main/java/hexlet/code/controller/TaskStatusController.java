package hexlet.code.controller;

import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusDTO;
import hexlet.code.dto.task_status.TaskStatusUpdateDTO;
import hexlet.code.service.TaskStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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

@Tag(name = "Task statuses", description = "Статусы задач")
@RestController
@RequestMapping("/api/task_statuses")
public class TaskStatusController {

    private final TaskStatusService service;

    public TaskStatusController(TaskStatusService service) {
        this.service = service;
    }

    @Operation(summary = "Список статусов")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<TaskStatusDTO>> index() {
        List<TaskStatusDTO> list = service.findAll();
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(list.size())).body(list);
    }

    @Operation(summary = "Статус по id")
    @SecurityRequirements
    @GetMapping("/{id}")
    public TaskStatusDTO show(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Создание статуса")
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PostMapping
    public ResponseEntity<TaskStatusDTO> create(@Valid @RequestBody TaskStatusCreateDTO dto) {
        var created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/task_statuses/" + created.id()))
                .body(created);
    }

    @Operation(
            summary = "Частичное обновление статуса",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    TaskStatusUpdateDTO.class))))
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskStatusDTO update(@PathVariable Long id, @RequestBody JsonNode patchNode) {
        return service.update(id, patchNode);
    }

    @Operation(summary = "Удаление статуса")
    @ApiResponse(responseCode = "409", description = "Статус используется в задачах")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
