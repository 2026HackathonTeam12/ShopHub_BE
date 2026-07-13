package org.hackathon12.shophub.infrastructure.instagram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "instagram.graph")
public record InstagramGraphProperties(
        String baseUrl,
        String accountId,
        String accessToken,
        Integer maxAttempts,
        Long intervalMs
) {
}
