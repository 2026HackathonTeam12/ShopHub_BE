package org.hackathon12.shophub.domain.ai.model;

public record ReviewReplyPrompt(
        String storeName,
        String reviewContent,
        int rating
) {
}
