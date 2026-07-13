package org.hackathon12.shophub.domain.ai.port;

import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.model.InstagramCaptionPrompt;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;

public interface AiTextGenerationPort {

    ContentSuggestion generateContentSuggestion(ContentSuggestionPrompt prompt);

    String generateInstagramCaption(InstagramCaptionPrompt prompt);

    String generateReviewReply(ReviewReplyPrompt prompt);
}
