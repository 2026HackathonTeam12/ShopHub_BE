package org.hackathon12.shophub.infrastructure.web.auth;

import org.hackathon12.shophub.domain.auth.service.AuthSessionService;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthContext {

    private final AuthSessionService authSessionService;

    public AuthContext(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    public Optional<UUID> resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        UUID userId = authSessionService.resolveUserId(authorization.substring("Bearer ".length()).trim());
        return Optional.ofNullable(userId);
    }
}
