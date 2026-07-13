package org.hackathon12.shophub.domain.ai.service;

import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.model.InstagramCaptionPrompt;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;
import org.hackathon12.shophub.domain.ai.port.AiTextGenerationPort;
import org.springframework.stereotype.Service;

@Service
public class AiTextGenerationService {

    private final AiTextGenerationPort aiTextGenerationPort;

    public AiTextGenerationService(AiTextGenerationPort aiTextGenerationPort) {
        this.aiTextGenerationPort = aiTextGenerationPort;
    }

    public ContentSuggestion generateContentSuggestion(ContentSuggestionPrompt prompt) {
        return aiTextGenerationPort.generateContentSuggestion(prompt);
    }

    public String generateInstagramCaption(InstagramCaptionPrompt prompt) {
        return aiTextGenerationPort.generateInstagramCaption(prompt);
    }

    public String generateReviewReply(ReviewReplyPrompt prompt) {
        return aiTextGenerationPort.generateReviewReply(prompt);
    }
}
