package org.hackathon12.shophub.infrastructure.x.oauth;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface XOAuthStateStore {

    void savePendingState(String state, UUID storeId, UUID userId, String codeVerifier, Duration ttl);

    Optional<XOAuthPendingState> consumePendingState(String state);

    void saveAccessToken(UUID storeId, String accessToken, Duration ttl);

    Optional<String> findAccessToken(UUID storeId);

    void deleteAccessToken(UUID storeId);
}
