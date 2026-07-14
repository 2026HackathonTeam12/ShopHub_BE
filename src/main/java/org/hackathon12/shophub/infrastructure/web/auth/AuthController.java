package org.hackathon12.shophub.infrastructure.web.auth;

import org.hackathon12.shophub.domain.auth.model.UserAccount;
import org.hackathon12.shophub.domain.auth.service.AuthService;
import org.hackathon12.shophub.domain.auth.service.AuthSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService authSessionService;

    public AuthController(AuthService authService, AuthSessionService authSessionService) {
        this.authService = authService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signUp(@RequestBody SignUpRequest request) {
        UserAccount account = authService.signUp(request.email(), request.password(), request.name());
        return toResponse(account);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        UserAccount account = authService.login(request.email(), request.password());
        return toResponse(account);
    }

    private AuthResponse toResponse(UserAccount account) {
        Instant expiresAt = Instant.now().plusSeconds(60L * 60L * 24L);
        return new AuthResponse(
                authSessionService.issueToken(account.id()),
                expiresAt,
                new UserProfile(account.id(), account.email(), account.name())
        );
    }

    public record SignUpRequest(
            String email,
            String password,
            String name
    ) {
    }

    public record LoginRequest(
            String email,
            String password
    ) {
    }

    public record AuthResponse(
            String accessToken,
            Instant expiresAt,
            UserProfile user
    ) {
    }

    public record UserProfile(
            UUID id,
            String email,
            String name
    ) {
    }
}
