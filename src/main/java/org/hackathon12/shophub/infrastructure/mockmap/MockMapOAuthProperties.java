package org.hackathon12.shophub.infrastructure.mockmap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock-map.oauth")
public record MockMapOAuthProperties(
        String clientId,
        String clientSecret,
        String refreshToken,
        String redirectUri,
        String authorizePath,
        String revokePath,
        String tokenStorePath
) {
}
