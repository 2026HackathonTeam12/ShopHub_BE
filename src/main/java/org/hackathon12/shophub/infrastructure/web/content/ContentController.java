package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentPlatformStatusItem;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.service.ContentChannelPublishService;
import org.hackathon12.shophub.domain.content.service.ContentService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/contents")
public class ContentController {

    private final ContentService contentService;
    private final ContentChannelPublishService contentChannelPublishService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public ContentController(
            ContentService contentService,
            ContentChannelPublishService contentChannelPublishService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.contentService = contentService;
        this.contentChannelPublishService = contentChannelPublishService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @GetMapping
    public List<ContentItem> getContents(
            @PathVariable UUID storeId,
            @RequestParam(required = false) ContentStatus status,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return contentService.getContents(storeId, status);
    }

    @GetMapping("/platform-status")
    public List<ContentPlatformStatusItem> getContentPlatformStatuses(
            @PathVariable UUID storeId,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return contentService.getContentPlatformStatuses(storeId);
    }

    @PostMapping
    public ResponseEntity<ContentItem> createContent(
            @PathVariable UUID storeId,
            @RequestBody CreateContentRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);

        if (!StringUtils.hasText(requestBody.title()) || !StringUtils.hasText(requestBody.body())) {
            throw new IllegalArgumentException("콘텐츠 생성에는 title/body가 필요합니다.");
        }
        List<String> channels = requestBody.channels() == null || requestBody.channels().isEmpty()
                ? List.of(ContentChannel.INSTAGRAM.name())
                : requestBody.channels();
        ContentItem created = contentService.createContent(storeId, requestBody.title(), requestBody.body(), channels);
        List<String> imgUrls = requestBody.img_urls() == null ? List.of() : requestBody.img_urls();
        created = contentChannelPublishService.publishToChannels(storeId, created, imgUrls);

        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/suggest")
    public ContentSuggestion suggestContent(
            @PathVariable UUID storeId,
            @RequestBody SuggestContentRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return contentService.suggestContent(storeId, requestBody.eventText());
    }

    @PostMapping("/{contentId}/retry")
    public ContentItem retry(
            @PathVariable UUID storeId,
            @PathVariable UUID contentId,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return contentService.retryContent(storeId, contentId);
    }

    @PatchMapping("/{contentId}/status")
    public ContentItem changeStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID contentId,
            @RequestBody ChangeContentStatusRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return contentService.updateStatus(storeId, contentId, requestBody.status());
    }

    public record CreateContentRequest(
            String title,
            String body,
            List<String> channels,
            @JsonProperty("img_urls") @JsonAlias("imageUrls") List<String> img_urls
    ) {
    }

    public record SuggestContentRequest(
            String eventText
    ) {
    }

    public record ChangeContentStatusRequest(
            ContentStatus status
    ) {
    }
}
