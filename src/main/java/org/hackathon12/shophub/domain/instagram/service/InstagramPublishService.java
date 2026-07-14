package org.hackathon12.shophub.domain.instagram.service;

import org.hackathon12.shophub.domain.ai.model.AiGeneratedText;
import org.hackathon12.shophub.domain.ai.model.InstagramCaptionPrompt;
import org.hackathon12.shophub.domain.ai.service.AiTextGenerationService;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.service.ContentService;
import org.hackathon12.shophub.domain.instagram.model.InstagramPublishResult;
import org.hackathon12.shophub.domain.instagram.port.InstagramPublishPort;
import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InstagramPublishService {

    private final StoreProfileService storeProfileService;
    private final ContentService contentService;
    private final AiTextGenerationService aiTextGenerationService;
    private final InstagramPublishPort instagramPublishPort;

    public InstagramPublishService(
            StoreProfileService storeProfileService,
            ContentService contentService,
            AiTextGenerationService aiTextGenerationService,
            InstagramPublishPort instagramPublishPort
    ) {
        this.storeProfileService = storeProfileService;
        this.contentService = contentService;
        this.aiTextGenerationService = aiTextGenerationService;
        this.instagramPublishPort = instagramPublishPort;
    }

    public InstagramPublishResult generateAndPublish(UUID storeId, String firstImageUrl, String secondImageUrl) {
        return generateAndPublish(storeId, List.of(firstImageUrl, secondImageUrl));
    }

    public void ensurePublishReady() {
        instagramPublishPort.ensurePublishReady();
    }

    public void requireStoreExists(UUID storeId) {
        storeProfileService.getStore(storeId);
    }

    public InstagramPublishResult generateAndPublish(UUID storeId, List<String> imageUrls) {
        ContentItem baseContent = selectBaseContent(storeId);
        return publishContent(storeId, baseContent, imageUrls);
    }

    public InstagramPublishResult publishContent(UUID storeId, ContentItem content, List<String> imageUrls) {
        StoreProfile store = storeProfileService.getStore(storeId);
        String caption = resolveCaption(storeId, store, content);
        String mediaId = instagramPublishPort.publishPost(caption, imageUrls);

        return new InstagramPublishResult(
                mediaId,
                caption,
                imageUrls,
                "https://www.instagram.com/commentcopybot/",
                Instant.now()
        );
    }

    private String resolveCaption(UUID storeId, StoreProfile store, ContentItem content) {
        if (StringUtils.hasText(content.body())) {
            if (StringUtils.hasText(content.title())) {
                return content.title() + "\n\n" + content.body();
            }
            return content.body();
        }

        InstagramCaptionPrompt prompt = new InstagramCaptionPrompt(
                store.name(),
                store.category(),
                store.toneOfVoice(),
                toBusinessHoursSummary(store.businessHours()),
                toMenuSummary(store),
                content.title(),
                content.body()
        );
        AiGeneratedText suggestion = aiTextGenerationService.suggestInstagramCaption(prompt);
        return suggestion.text();
    }

    private ContentItem selectBaseContent(UUID storeId) {
        List<ContentItem> drafts = contentService.getContents(storeId, ContentStatus.DRAFT);
        if (!drafts.isEmpty()) {
            return drafts.get(0);
        }
        List<ContentItem> all = contentService.getContents(storeId, null);
        if (!all.isEmpty()) {
            return all.get(0);
        }

        return new ContentItem(
                UUID.randomUUID(),
                storeId,
                "오늘의 매장 소식",
                "매장 운영 정보와 메뉴 소식을 전합니다.",
                List.of(ContentChannel.INSTAGRAM.name()),
                ContentStatus.DRAFT,
                Instant.now(),
                ContentItem.pendingPlatformsFor(List.of(ContentChannel.INSTAGRAM.name()))
        );
    }

    private String toBusinessHoursSummary(List<BusinessHour> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) {
            return "운영시간 정보 없음";
        }

        return businessHours.stream()
                .filter(BusinessHour::open)
                .map(hour -> hour.dayOfWeek() + " " + hour.openTime() + "-" + hour.closeTime())
                .collect(Collectors.joining(", "));
    }

    private String toMenuSummary(StoreProfile store) {
        if (store.menuItems() == null || store.menuItems().isEmpty()) {
            return "대표 메뉴 정보 없음";
        }
        return store.menuItems().stream()
                .limit(3)
                .map(menu -> menu.name())
                .collect(Collectors.joining(", "));
    }
}
