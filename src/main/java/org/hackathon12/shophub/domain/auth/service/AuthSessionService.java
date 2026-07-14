package org.hackathon12.shophub.domain.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public String issueToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionEntry(userId, Instant.now().plusSeconds(60L * 60L * 24L)));
        return token;
    }

    public UUID resolveUserId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        SessionEntry entry = sessions.get(accessToken);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            sessions.remove(accessToken);
            return null;
        }
        return entry.userId();
    }

    private record SessionEntry(UUID userId, Instant expiresAt) {
    }
}
