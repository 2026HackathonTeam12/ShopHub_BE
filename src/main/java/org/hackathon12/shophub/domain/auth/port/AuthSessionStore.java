package org.hackathon12.shophub.domain.auth.port;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionStore {

    void save(String accessToken, UUID userId, Duration ttl);

    Optional<UUID> findUserId(String accessToken);

    void delete(String accessToken);
}
