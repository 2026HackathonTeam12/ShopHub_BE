package org.hackathon12.shophub.infrastructure.auth;

import org.hackathon12.shophub.domain.auth.port.AuthSessionStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.auth.session", name = "store", havingValue = "memory")
public class InMemoryAuthSessionStore implements AuthSessionStore {

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(String accessToken, UUID userId, Duration ttl) {
        sessions.put(accessToken, new SessionEntry(userId, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<UUID> findUserId(String accessToken) {
        SessionEntry entry = sessions.get(accessToken);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            sessions.remove(accessToken);
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    @Override
    public void delete(String accessToken) {
        sessions.remove(accessToken);
    }

    private record SessionEntry(UUID userId, Instant expiresAt) {
    }
}
