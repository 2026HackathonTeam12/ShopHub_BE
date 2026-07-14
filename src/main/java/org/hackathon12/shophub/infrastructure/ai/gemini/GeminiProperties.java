package org.hackathon12.shophub.infrastructure.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model,
        Double temperature,
        Integer maxOutputTokens
) {
}
