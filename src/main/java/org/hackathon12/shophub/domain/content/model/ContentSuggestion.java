package org.hackathon12.shophub.domain.content.model;

import org.hackathon12.shophub.domain.ai.model.AiGenerationSource;

public record ContentSuggestion(
        String title,
        String body,
        AiGenerationSource source
) {
}
