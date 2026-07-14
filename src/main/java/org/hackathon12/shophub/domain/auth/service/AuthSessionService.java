package org.hackathon12.shophub.domain.auth.service;

import org.hackathon12.shophub.domain.auth.port.AuthSessionStore;
import org.hackathon12.shophub.global.config.AuthSessionProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthSessionService {

    private final AuthSessionStore authSessionStore;
    private final AuthSessionProperties authSessionProperties;

    public AuthSessionService(AuthSessionStore authSessionStore, AuthSessionProperties authSessionProperties) {
        this.authSessionStore = authSessionStore;
        this.authSessionProperties = authSessionProperties;
    }

    public String issueToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        authSessionStore.save(token, userId, authSessionProperties.ttl());
        return token;
    }

    public UUID resolveUserId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return authSessionStore.findUserId(accessToken).orElse(null);
    }

    public void revokeToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        authSessionStore.delete(accessToken);
    }
}
