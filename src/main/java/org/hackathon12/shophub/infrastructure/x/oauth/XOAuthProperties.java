package org.hackathon12.shophub.infrastructure.x.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "x.oauth")
public record XOAuthProperties(
        String authorizeUrl,
        String tokenUrl,
        String revokeUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        String scope
) {
}
