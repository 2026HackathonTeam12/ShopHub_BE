package org.hackathon12.shophub.infrastructure.web.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.hackathon12.shophub.domain.auth.model.UserAccount;
import org.hackathon12.shophub.domain.auth.service.AuthService;
import org.hackathon12.shophub.domain.auth.service.AuthSessionService;
import org.hackathon12.shophub.global.config.AuthSessionProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService authSessionService;
    private final AuthSessionProperties authSessionProperties;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public AuthController(
            AuthService authService,
            AuthSessionService authSessionService,
            AuthSessionProperties authSessionProperties,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.authService = authService;
        this.authSessionService = authSessionService;
        this.authSessionProperties = authSessionProperties;
        this.shopHubAuthGuard = shopHubAuthGuard;
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

    @GetMapping("/me")
    @Operation(
            summary = "현재 사용자 조회",
            description = "Bearer 토큰으로 인증된 사용자의 프로필을 반환합니다.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public UserProfile me(HttpServletRequest request) {
        UUID userId = shopHubAuthGuard.requireUserId(request);
        UserAccount account = authService.getAccount(userId);
        return new UserProfile(account.id(), account.email(), account.name());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authSessionService.revokeToken(authorization.substring("Bearer ".length()).trim());
        }
    }

    private AuthResponse toResponse(UserAccount account) {
        Instant expiresAt = Instant.now().plus(authSessionProperties.ttl());
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
