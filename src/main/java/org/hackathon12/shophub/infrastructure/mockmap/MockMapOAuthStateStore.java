package org.hackathon12.shophub.infrastructure.mockmap;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface MockMapOAuthStateStore {

    void savePendingState(String state, UUID storeId, UUID userId, Duration ttl);

    Optional<MockMapOAuthPendingState> consumePendingState(String state);

    void saveAccessToken(UUID storeId, String accessToken, Duration ttl);

    Optional<String> findAccessToken(UUID storeId);

    void deleteAccessToken(UUID storeId);
}
