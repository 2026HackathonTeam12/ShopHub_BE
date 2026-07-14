package org.hackathon12.shophub.domain.content.service;

import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.service.AiTextGenerationService;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.port.ContentPort;
import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private final ContentPort contentPort;
    private final StoreProfileService storeProfileService;
    private final AiTextGenerationService aiTextGenerationService;

    public ContentService(
            ContentPort contentPort,
            StoreProfileService storeProfileService,
            AiTextGenerationService aiTextGenerationService
    ) {
        this.contentPort = contentPort;
        this.storeProfileService = storeProfileService;
        this.aiTextGenerationService = aiTextGenerationService;
    }

    public List<ContentItem> getContents(UUID storeId, ContentStatus status) {
        storeProfileService.getStore(storeId);
        List<ContentItem> items = contentPort.findByStoreId(storeId);
        if (status == null) {
            return items;
        }
        return items.stream()
                .filter(item -> item.status() == status)
                .toList();
    }

    public ContentItem createContent(UUID storeId, String title, String body, List<String> channels) {
        storeProfileService.getStore(storeId);
        ContentItem contentItem = new ContentItem(
                UUID.randomUUID(),
                storeId,
                title,
                body,
                channels,
                ContentStatus.DRAFT,
                Instant.now()
        );
        return contentPort.save(contentItem);
    }

    public ContentSuggestion suggestContent(UUID storeId, String eventText) {
        StoreProfile storeProfile = storeProfileService.getStore(storeId);

        ContentSuggestionPrompt prompt = new ContentSuggestionPrompt(
                storeProfile.name(),
                storeProfile.category(),
                storeProfile.toneOfVoice(),
                toBusinessHoursSummary(storeProfile.businessHours()),
                toMenuSummary(storeProfile),
                StringUtils.hasText(eventText) ? eventText.trim() : "진행 중인 이벤트 정보 없음"
        );
        return aiTextGenerationService.suggestContent(prompt);
    }

    public ContentItem retryContent(UUID storeId, UUID contentId) {
        ContentItem current = contentPort.findById(contentId);
        if (current == null) {
            throw new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId);
        }
        ensureContentOwnedByStore(current, storeId);

        ContentItem retried = new ContentItem(
                current.id(),
                current.storeId(),
                current.title(),
                current.body(),
                current.channels(),
                ContentStatus.DRAFT,
                Instant.now()
        );
        return contentPort.save(retried);
    }

    public ContentItem updateStatus(UUID storeId, UUID contentId, ContentStatus status) {
        ContentItem current = contentPort.findById(contentId);
        if (current == null) {
            throw new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId);
        }
        ensureContentOwnedByStore(current, storeId);

        ContentItem updated = new ContentItem(
                current.id(),
                current.storeId(),
                current.title(),
                current.body(),
                current.channels(),
                status,
                Instant.now()
        );
        return contentPort.save(updated);
    }

    private void ensureContentOwnedByStore(ContentItem contentItem, UUID storeId) {
        if (!contentItem.storeId().equals(storeId)) {
            throw new NotFoundException("콘텐츠를 찾을 수 없습니다. storeId=" + storeId + ", contentId=" + contentItem.id());
        }
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

    private String toMenuSummary(StoreProfile storeProfile) {
        if (storeProfile.menuItems() == null || storeProfile.menuItems().isEmpty()) {
            return "대표 메뉴 정보 없음";
        }
        return storeProfile.menuItems().stream()
                .limit(3)
                .map(menu -> menu.name())
                .collect(Collectors.joining(", "));
    }
}
