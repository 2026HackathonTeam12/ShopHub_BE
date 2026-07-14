package org.hackathon12.shophub.infrastructure.mockmap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock-map.api")
public record MockMapApiProperties(
        String baseUrl,
        String healthPath,
        String oauthTokenPath,
        String reviewsPath
) {
}
