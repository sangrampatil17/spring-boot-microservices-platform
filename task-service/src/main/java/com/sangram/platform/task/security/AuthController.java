package com.sangram.platform.task.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo login endpoint backed by two in-memory users (ADMIN and USER) configured via properties.
 * In a real system this would delegate to a user store or an identity provider.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }

    private record DemoUser(String passwordHash, List<String> roles) { }

    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final Map<String, DemoUser> users;

    public AuthController(JwtService jwtService, PasswordEncoder encoder,
                          @Value("${app.demo.admin-password}") String adminPassword,
                          @Value("${app.demo.user-password}") String userPassword) {
        this.jwtService = jwtService;
        this.users = Map.of(
                "admin", new DemoUser(encoder.encode(adminPassword), List.of("ADMIN", "USER")),
                "user", new DemoUser(encoder.encode(userPassword), List.of("USER")));
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        DemoUser user = users.get(request.username());
        if (user == null || !encoder.matches(request.password(), user.passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid credentials"));
        }
        return ResponseEntity.ok(Map.of("token", jwtService.issue(request.username(), user.roles())));
    }
}
