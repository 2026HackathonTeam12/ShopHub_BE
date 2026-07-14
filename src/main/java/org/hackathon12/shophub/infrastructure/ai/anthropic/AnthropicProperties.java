package org.hackathon12.shophub.infrastructure.ai.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(
        String apiKey,
        String baseUrl,
        String model,
        Integer maxTokens,
        String apiVersion
) {
}
