package org.hackathon12.shophub.infrastructure.facebook.oauth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FacebookOAuthStateStore {

    private final Map<String, TimedPendingState> pendingStates = new ConcurrentHashMap<>();

    public void savePendingState(String state, UUID storeId, UUID userId, Duration ttl) {
        pendingStates.put(state, new TimedPendingState(storeId, userId, Instant.now().plus(ttl)));
    }

    public Optional<FacebookOAuthPendingState> consumePendingState(String state) {
        TimedPendingState pending = pendingStates.remove(state);
        if (pending == null || Instant.now().isAfter(pending.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(new FacebookOAuthPendingState(pending.storeId(), pending.userId()));
    }

    private record TimedPendingState(UUID storeId, UUID userId, Instant expiresAt) {
    }
}
