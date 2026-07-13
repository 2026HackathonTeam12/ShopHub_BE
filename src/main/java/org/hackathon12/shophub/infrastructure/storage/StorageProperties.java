package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String uploadDir,
        String publicBaseUrl
) {
}
