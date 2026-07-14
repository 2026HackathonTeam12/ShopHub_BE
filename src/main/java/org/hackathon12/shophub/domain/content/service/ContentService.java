package org.hackathon12.shophub.domain.content.service;

import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.service.AiTextGenerationService;
import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentPlatformStatusItem;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private static final Map<ContentStatus, EnumSet<ContentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ContentStatus.DRAFT, EnumSet.of(ContentStatus.SCHEDULED, ContentStatus.PUBLISHED),
            ContentStatus.SCHEDULED, EnumSet.of(ContentStatus.DRAFT, ContentStatus.PUBLISHED),
            ContentStatus.FAILED, EnumSet.of(ContentStatus.DRAFT),
            ContentStatus.PUBLISHED, EnumSet.noneOf(ContentStatus.class)
    );

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

    public List<ContentPlatformStatusItem> getContentPlatformStatuses(UUID storeId) {
        storeProfileService.getStore(storeId);
        return contentPort.findPlatformStatusesByStoreId(storeId);
    }

    public ContentItem createContent(UUID storeId, String title, String body, List<String> channels) {
        storeProfileService.getStore(storeId);
        List<String> normalizedChannels = ContentChannel.normalizeAll(channels);
        ContentItem contentItem = new ContentItem(
                UUID.randomUUID(),
                storeId,
                title,
                body,
                normalizedChannels,
                ContentStatus.DRAFT,
                Instant.now(),
                ContentItem.pendingPlatformsFor(normalizedChannels)
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
        if (current.status() != ContentStatus.FAILED) {
            throw new IllegalArgumentException(
                    "retry는 FAILED 상태의 콘텐츠만 가능합니다. currentStatus=" + current.status()
            );
        }
        validateStatusTransition(current.status(), ContentStatus.DRAFT);

        contentPort.resetPlatformStatusesToPending(contentId);

        ContentItem retried = new ContentItem(
                current.id(),
                current.storeId(),
                current.title(),
                current.body(),
                current.channels(),
                ContentStatus.DRAFT,
                Instant.now(),
                ContentItem.pendingPlatformsFor(current.channels())
        );
        return contentPort.save(retried);
    }

    public ContentItem updateStatus(UUID storeId, UUID contentId, ContentStatus status) {
        ContentItem current = contentPort.findById(contentId);
        if (current == null) {
            throw new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId);
        }
        ensureContentOwnedByStore(current, storeId);
        validateStatusTransition(current.status(), status);

        ContentItem updated = new ContentItem(
                current.id(),
                current.storeId(),
                current.title(),
                current.body(),
                current.channels(),
                status,
                Instant.now(),
                current.platforms()
        );
        return contentPort.save(updated);
    }

    private void ensureContentOwnedByStore(ContentItem contentItem, UUID storeId) {
        if (!contentItem.storeId().equals(storeId)) {
            throw new NotFoundException("콘텐츠를 찾을 수 없습니다. storeId=" + storeId + ", contentId=" + contentItem.id());
        }
    }

    private void validateStatusTransition(ContentStatus currentStatus, ContentStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }

        EnumSet<ContentStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.getOrDefault(
                currentStatus,
                EnumSet.noneOf(ContentStatus.class)
        );
        if (!allowedNextStatuses.contains(nextStatus)) {
            throw new IllegalArgumentException(
                    "허용되지 않는 콘텐츠 상태 전이입니다. currentStatus=" + currentStatus + ", nextStatus=" + nextStatus
            );
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
