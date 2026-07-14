package org.hackathon12.shophub.infrastructure.x;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "x.api")
public record XApiProperties(
        String tweetsUrl,
        String mediaUploadUrl
) {
}
