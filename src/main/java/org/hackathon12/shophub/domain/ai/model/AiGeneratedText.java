package org.hackathon12.shophub.domain.ai.model;

public record AiGeneratedText(
        String text,
        AiGenerationSource source
) {
}
