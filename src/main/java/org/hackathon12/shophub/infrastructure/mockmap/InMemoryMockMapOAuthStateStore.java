package org.hackathon12.shophub.infrastructure.mockmap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "mock-map.oauth.cache", name = "store", havingValue = "memory")
public class InMemoryMockMapOAuthStateStore implements MockMapOAuthStateStore {

    private final Map<String, TimedPendingState> pendingStates = new ConcurrentHashMap<>();
    private final Map<UUID, TimedAccessToken> accessTokens = new ConcurrentHashMap<>();

    @Override
    public void savePendingState(String state, UUID storeId, UUID userId, Duration ttl) {
        pendingStates.put(state, new TimedPendingState(storeId, userId, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<MockMapOAuthPendingState> consumePendingState(String state) {
        TimedPendingState pending = pendingStates.remove(state);
        if (pending == null || Instant.now().isAfter(pending.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(new MockMapOAuthPendingState(pending.storeId(), pending.userId()));
    }

    @Override
    public void saveAccessToken(UUID storeId, String accessToken, Duration ttl) {
        accessTokens.put(storeId, new TimedAccessToken(accessToken, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> findAccessToken(UUID storeId) {
        TimedAccessToken cached = accessTokens.get(storeId);
        if (cached == null || Instant.now().isAfter(cached.expiresAt())) {
            accessTokens.remove(storeId);
            return Optional.empty();
        }
        return Optional.of(cached.accessToken());
    }

    @Override
    public void deleteAccessToken(UUID storeId) {
        accessTokens.remove(storeId);
    }

    private record TimedPendingState(UUID storeId, UUID userId, Instant expiresAt) {
    }

    private record TimedAccessToken(String accessToken, Instant expiresAt) {
    }
}
