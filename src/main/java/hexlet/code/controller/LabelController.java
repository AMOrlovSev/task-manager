package hexlet.code.controller;

import hexlet.code.dto.label.LabelCreateDTO;
import hexlet.code.dto.label.LabelDTO;
import hexlet.code.dto.label.LabelUpdateDTO;
import hexlet.code.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@Tag(name = "Labels", description = "Метки задач")
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @Operation(summary = "Список меток")
    @ApiResponse(
            responseCode = "200",
            headers = @Header(name = "X-Total-Count", description = "Всего записей"))
    @GetMapping
    public ResponseEntity<List<LabelDTO>> index() {
        var labels = labelService.findAll();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(labels.size()))
                .body(labels);
    }

    @Operation(summary = "Метка по id")
    @GetMapping("/{id}")
    public LabelDTO show(@PathVariable Long id) {
        return labelService.findById(id);
    }

    @Operation(summary = "Создание метки")
    @ApiResponse(responseCode = "201", description = "Метка создана")
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PostMapping
    public ResponseEntity<LabelDTO> create(@Valid @RequestBody LabelCreateDTO dto) {
        var created = labelService.create(dto);
        return ResponseEntity.created(URI.create("/api/labels/" + created.id())).body(created);
    }

    @Operation(
            summary = "Частичное обновление метки",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    LabelUpdateDTO.class))))
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LabelDTO update(@PathVariable Long id, @RequestBody JsonNode patch) {
        return labelService.update(id, patch);
    }

    @Operation(summary = "Удаление метки")
    @ApiResponse(responseCode = "204", description = "Метка удалена")
    @ApiResponse(responseCode = "409", description = "Метка используется в задачах")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
