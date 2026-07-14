package org.hackathon12.shophub.infrastructure.ai.template;

import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.model.InstagramCaptionPrompt;
import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiTextTemplateProvider {

    public TemplateContent contentSuggestion(ContentSuggestionPrompt prompt) {
        return new TemplateContent(
                safe(prompt.storeName()) + " 오늘의 소식",
                "%s 관련 안내드립니다. %s\n운영시간은 %s이며, 대표 메뉴는 %s입니다."
                        .formatted(
                                safe(prompt.category()),
                                safe(prompt.eventText()),
                                safe(prompt.businessHoursSummary()),
                                safe(prompt.menuSummary())
                        )
        );
    }

    public String instagramCaption(InstagramCaptionPrompt prompt) {
        return "%s의 오늘 추천 소식을 전해드립니다. %s %s\n운영시간은 %s이며, 편하게 들러주세요."
                .formatted(
                        safe(prompt.storeName()),
                        safe(prompt.contentTitle()),
                        safe(prompt.contentBody()),
                        safe(prompt.businessHoursSummary())
                );
    }

    public String reviewReply(ReviewReplyPrompt prompt) {
        if (prompt.rating() <= 2) {
            return "소중한 의견 남겨주셔서 감사합니다. 불편을 드려 죄송하며 말씀해주신 부분을 빠르게 점검하고 개선하겠습니다.";
        }
        if (prompt.rating() == 3) {
            return "리뷰 남겨주셔서 감사합니다. 남겨주신 의견을 반영해 더 만족스러운 방문 경험을 드릴 수 있도록 노력하겠습니다.";
        }
        return "따뜻한 리뷰 감사합니다. 다음 방문에서도 편안하고 좋은 경험을 드릴 수 있도록 정성껏 준비하겠습니다.";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    public record TemplateContent(
            String title,
            String body
    ) {
    }
}
