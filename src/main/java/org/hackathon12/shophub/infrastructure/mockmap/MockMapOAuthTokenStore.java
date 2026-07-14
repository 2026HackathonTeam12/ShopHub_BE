package org.hackathon12.shophub.infrastructure.mockmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
public class MockMapOAuthTokenStore {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMapOAuthProperties properties;

    public MockMapOAuthTokenStore(MockMapOAuthProperties properties) {
        this.properties = properties;
    }

    public synchronized StoredTokens read() {
        Path storePath = resolveStorePath();
        if (!Files.exists(storePath)) {
            return bootstrapFromProperties();
        }

        try {
            StoredTokens stored = objectMapper.readValue(storePath.toFile(), StoredTokens.class);
            if (stored == null || !StringUtils.hasText(stored.refreshToken())) {
                return bootstrapFromProperties();
            }
            return stored;
        } catch (IOException exception) {
            throw new MockMapApiException("MockMap OAuth token store를 읽을 수 없습니다.", exception);
        }
    }

    public synchronized void write(StoredTokens tokens) {
        Path storePath = resolveStorePath();
        try {
            Files.createDirectories(storePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), tokens);
        } catch (IOException exception) {
            throw new MockMapApiException("MockMap OAuth token store를 저장할 수 없습니다.", exception);
        }
    }

    public synchronized void clear() {
        Path storePath = resolveStorePath();
        try {
            Files.deleteIfExists(storePath);
        } catch (IOException exception) {
            throw new MockMapApiException("MockMap OAuth token store를 삭제할 수 없습니다.", exception);
        }
    }

    private StoredTokens bootstrapFromProperties() {
        if (!StringUtils.hasText(properties.refreshToken())) {
            return StoredTokens.empty();
        }
        return new StoredTokens(
                properties.refreshToken(),
                null,
                properties.clientId(),
                null,
                null,
                Instant.now().toString()
        );
    }

    private Path resolveStorePath() {
        return Path.of(properties.tokenStorePath()).toAbsolutePath().normalize();
    }

    public record StoredTokens(
            String refreshToken,
            String accessToken,
            String clientId,
            String placeId,
            String placeName,
            String updatedAt
    ) {
        public static StoredTokens empty() {
            return new StoredTokens(null, null, null, null, null, null);
        }

        public boolean connected() {
            return StringUtils.hasText(refreshToken);
        }
    }
}
