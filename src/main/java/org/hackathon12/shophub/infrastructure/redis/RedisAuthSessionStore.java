package org.hackathon12.shophub.infrastructure.redis;

import org.hackathon12.shophub.domain.auth.port.AuthSessionStore;
import org.hackathon12.shophub.global.config.AuthSessionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.auth.session", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisAuthSessionStore implements AuthSessionStore {

    private final StringRedisTemplate redisTemplate;
    private final AuthSessionProperties authSessionProperties;

    public RedisAuthSessionStore(
            StringRedisTemplate redisTemplate,
            AuthSessionProperties authSessionProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.authSessionProperties = authSessionProperties;
    }

    @Override
    public void save(String accessToken, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(sessionKey(accessToken), userId.toString(), ttl);
    }

    @Override
    public Optional<UUID> findUserId(String accessToken) {
        String userId = redisTemplate.opsForValue().get(sessionKey(accessToken));
        if (!StringUtils.hasText(userId)) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(userId));
    }

    @Override
    public void delete(String accessToken) {
        redisTemplate.delete(sessionKey(accessToken));
    }

    private String sessionKey(String accessToken) {
        return authSessionProperties.getKeyPrefix() + accessToken;
    }
}
