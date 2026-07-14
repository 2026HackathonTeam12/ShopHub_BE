package org.hackathon12.shophub.global.config;

import org.hackathon12.shophub.domain.ai.port.AiTextGenerationPort;
import org.hackathon12.shophub.infrastructure.ai.anthropic.AnthropicProperties;
import org.hackathon12.shophub.infrastructure.ai.anthropic.AnthropicTextGenerationAdapter;
import org.hackathon12.shophub.infrastructure.ai.gemini.GeminiProperties;
import org.hackathon12.shophub.infrastructure.ai.gemini.GeminiTextGenerationAdapter;
import org.hackathon12.shophub.infrastructure.ai.openai.OpenAiProperties;
import org.hackathon12.shophub.infrastructure.ai.openai.OpenAiTextGenerationAdapter;
import org.hackathon12.shophub.infrastructure.ai.template.AiTextTemplateProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class AiTextGenerationConfiguration {

    @Bean
    public AiTextGenerationPort aiTextGenerationPort(
            RestClient.Builder restClientBuilder,
            GeminiProperties geminiProperties,
            AnthropicProperties anthropicProperties,
            OpenAiProperties openAiProperties,
            AiTextTemplateProvider templateProvider
    ) {
        if (StringUtils.hasText(geminiProperties.apiKey())) {
            return new GeminiTextGenerationAdapter(geminiProperties, templateProvider);
        }
        if (StringUtils.hasText(anthropicProperties.apiKey())) {
            return new AnthropicTextGenerationAdapter(restClientBuilder, anthropicProperties, templateProvider);
        }
        return new OpenAiTextGenerationAdapter(restClientBuilder, openAiProperties, templateProvider);
    }
}
