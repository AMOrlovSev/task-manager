package hexlet.code.controller;

import hexlet.code.dto.auth.AuthRequest;
import hexlet.code.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Получение JWT-токена")
@RestController
@RequestMapping("/api/login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Аутентификация и выдача JWT")
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<String> login(@RequestBody AuthRequest request) {
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(token);
    }
}
