package hexlet.code.controller;

import hexlet.code.component.PageableFactory;
import hexlet.code.dto.common.ListParams;
import hexlet.code.dto.common.PageResponses;
import hexlet.code.dto.common.SortField;
import hexlet.code.dto.user.UserCreateDTO;
import hexlet.code.dto.user.UserDTO;
import hexlet.code.dto.user.UserUpdateDTO;
import hexlet.code.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Users", description = "Управление пользователями")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Map<String, SortField> SORT_FIELDS =
            Map.of(
                    "id", SortField.of("id"),
                    "email", SortField.text("email"),
                    "firstName", SortField.text("firstName"),
                    "lastName", SortField.text("lastName"),
                    "createdAt", SortField.of("createdAt"));

    private final UserService userService;
    private final PageableFactory pageableFactory;

    public UserController(UserService userService, PageableFactory pageableFactory) {
        this.userService = userService;
        this.pageableFactory = pageableFactory;
    }

    @Operation(summary = "Список пользователей с пагинацией и сортировкой")
    @ApiResponse(
            responseCode = "200",
            headers = @Header(name = "X-Total-Count", description = "Всего записей"))
    @ApiResponse(responseCode = "400", description = "Невалидные параметры пагинации/сортировки")
    @GetMapping
    public ResponseEntity<List<UserDTO>> index(@ParameterObject ListParams params) {
        var pageable = pageableFactory.create(params, SORT_FIELDS);
        return PageResponses.of(userService.findAll(pageable));
    }

    @Operation(summary = "Пользователь по id")
    @GetMapping("/{id}")
    public UserDTO show(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "Регистрация пользователя")
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "Пользователь создан")
    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody UserCreateDTO dto) {
        UserDTO created = userService.create(dto);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @Operation(
            summary = "Частичное обновление пользователя",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(implementation = UserUpdateDTO.class))))
    @ApiResponse(responseCode = "400", description = "Невалидное тело запроса")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("#id.toString() == authentication.name")
    public UserDTO update(@PathVariable Long id, @RequestBody JsonNode patchNode) {
        return userService.update(id, patchNode);
    }

    @Operation(summary = "Удаление пользователя")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("#id.toString() == authentication.name")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
