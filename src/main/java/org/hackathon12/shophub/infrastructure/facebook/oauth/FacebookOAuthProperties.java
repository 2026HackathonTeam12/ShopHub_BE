package org.hackathon12.shophub.infrastructure.facebook.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facebook.oauth")
public record FacebookOAuthProperties(
        String redirectUri,
        Long pendingStateTtlSeconds
) {
}
