package hexlet.code.controller;

import hexlet.code.component.PageableFactory;
import hexlet.code.dto.common.ListParams;
import hexlet.code.dto.common.PageResponses;
import hexlet.code.dto.common.SortField;
import hexlet.code.dto.task_status.TaskStatusCreateDTO;
import hexlet.code.dto.task_status.TaskStatusDTO;
import hexlet.code.dto.task_status.TaskStatusUpdateDTO;
import hexlet.code.service.TaskStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
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

@Tag(name = "Task statuses", description = "Статусы задач")
@RestController
@RequestMapping("/api/task_statuses")
public class TaskStatusController {

    private static final Map<String, SortField> SORT_FIELDS =
            Map.of(
                    "id", SortField.of("id"),
                    "name", SortField.text("name"),
                    "slug", SortField.of("slug"),
                    "createdAt", SortField.of("createdAt"));

    private final TaskStatusService service;
    private final PageableFactory pageableFactory;

    public TaskStatusController(TaskStatusService service, PageableFactory pageableFactory) {
        this.service = service;
        this.pageableFactory = pageableFactory;
    }

    @Operation(summary = "Список статусов с пагинацией и сортировкой")
    @ApiResponse(
            responseCode = "200",
            headers = @Header(name = "X-Total-Count", description = "Всего записей"))
    @ApiResponse(responseCode = "400", description = "Невалидные параметры пагинации/сортировки")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<TaskStatusDTO>> index(@ParameterObject ListParams params) {
        var pageable = pageableFactory.create(params, SORT_FIELDS);
        return PageResponses.of(service.findAll(pageable));
    }

    @Operation(summary = "Статус по id")
    @SecurityRequirements
    @GetMapping("/{id}")
    public TaskStatusDTO show(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Создание статуса")
    @ApiResponse(responseCode = "201", description = "Статус создан")
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
