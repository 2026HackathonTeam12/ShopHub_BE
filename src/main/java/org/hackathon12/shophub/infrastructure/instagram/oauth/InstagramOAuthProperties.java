package org.hackathon12.shophub.infrastructure.instagram.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "instagram.oauth")
public record InstagramOAuthProperties(
        String redirectUri,
        Long pendingStateTtlSeconds
) {
}
