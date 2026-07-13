package org.hackathon12.shophub.infrastructure.ai.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        Double temperature
) {
}
