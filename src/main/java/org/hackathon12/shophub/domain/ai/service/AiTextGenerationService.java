package org.hackathon12.shophub.domain.ai.service;

import org.hackathon12.shophub.domain.ai.model.AiGeneratedText;
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

    public ContentSuggestion suggestContent(ContentSuggestionPrompt prompt) {
        return aiTextGenerationPort.generateContentSuggestion(prompt);
    }

    public AiGeneratedText suggestInstagramCaption(InstagramCaptionPrompt prompt) {
        return aiTextGenerationPort.generateInstagramCaption(prompt);
    }

    public AiGeneratedText suggestReviewReply(ReviewReplyPrompt prompt) {
        return aiTextGenerationPort.generateReviewReply(prompt);
    }
}
