package ru.amigo.axioma.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.amigo.axioma.config.JwtProperties;
import ru.amigo.axioma.security.dto.LoginRequest;
import ru.amigo.axioma.security.dto.LoginResponse;

/**
 * @author denis.chernonozhkin
 * @since 17.10.2026 20:26
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "Логин и выдача JWT-токена")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @Operation(summary = "Войти в систему", description = "Возвращает JWT-токен для доступа к API")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(LoginResponse.of(token, jwtProperties.expirationMs()));
    }
}
