package org.hackathon12.shophub.infrastructure.mockmap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mock-map.oauth.cache", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisMockMapOAuthStateStore implements MockMapOAuthStateStore {

    private final StringRedisTemplate redisTemplate;
    private final MockMapOAuthCacheProperties cacheProperties;

    public RedisMockMapOAuthStateStore(
            StringRedisTemplate redisTemplate,
            MockMapOAuthCacheProperties cacheProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public void savePendingState(String state, UUID storeId, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(
                pendingStateKey(state),
                storeId + "|" + userId,
                ttl
        );
    }

    @Override
    public Optional<MockMapOAuthPendingState> consumePendingState(String state) {
        String value = redisTemplate.opsForValue().getAndDelete(pendingStateKey(state));
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new MockMapOAuthPendingState(UUID.fromString(parts[0]), UUID.fromString(parts[1])));
    }

    @Override
    public void saveAccessToken(UUID storeId, String accessToken, Duration ttl) {
        redisTemplate.opsForValue().set(accessTokenKey(storeId), accessToken, ttl);
    }

    @Override
    public Optional<String> findAccessToken(UUID storeId) {
        String accessToken = redisTemplate.opsForValue().get(accessTokenKey(storeId));
        if (!StringUtils.hasText(accessToken)) {
            return Optional.empty();
        }
        return Optional.of(accessToken);
    }

    @Override
    public void deleteAccessToken(UUID storeId) {
        redisTemplate.delete(accessTokenKey(storeId));
    }

    private String pendingStateKey(String state) {
        return cacheProperties.getPendingStateKeyPrefix() + state;
    }

    private String accessTokenKey(UUID storeId) {
        return cacheProperties.getAccessTokenKeyPrefix() + storeId;
    }
}
