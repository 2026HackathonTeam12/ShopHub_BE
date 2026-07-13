package org.hackathon12.shophub.domain.ai.model;

public record InstagramCaptionPrompt(
        String storeName,
        String category,
        String toneOfVoice,
        String businessHoursSummary,
        String menuSummary,
        String contentTitle,
        String contentBody
) {
}
