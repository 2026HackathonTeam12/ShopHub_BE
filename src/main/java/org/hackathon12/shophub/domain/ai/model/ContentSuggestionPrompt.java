package org.hackathon12.shophub.domain.ai.model;

public record ContentSuggestionPrompt(
        String storeName,
        String category,
        String toneOfVoice,
        String businessHoursSummary,
        String menuSummary,
        String eventText
) {
}
