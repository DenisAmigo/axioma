package ru.amigo.axioma.security;

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
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(LoginResponse.of(token, jwtProperties.expirationMs()));
    }
}
