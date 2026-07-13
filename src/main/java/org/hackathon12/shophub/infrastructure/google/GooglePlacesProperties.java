package org.hackathon12.shophub.infrastructure.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.places")
public record GooglePlacesProperties(
        String apiKey,
        String baseUrl,
        String language
) {
}
