package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.service.ContentService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<ContentItem> getContents(
            @PathVariable UUID storeId,
            @RequestParam(required = false) ContentStatus status
    ) {
        return contentService.getContents(storeId, status);
    }

    @PostMapping
    public ResponseEntity<Object> createContent(@PathVariable UUID storeId, @RequestBody CreateContentRequest request) {
        if (Boolean.TRUE.equals(request.aiSuggest())) {
            ContentSuggestion suggestion = contentService.suggestContent(storeId, request.eventText());
            return ResponseEntity.ok(suggestion);
        }

        if (!StringUtils.hasText(request.title()) || !StringUtils.hasText(request.body())) {
            throw new IllegalArgumentException("콘텐츠 생성에는 title/body가 필요합니다.");
        }
        List<String> channels = request.channels() == null || request.channels().isEmpty()
                ? List.of("Instagram")
                : request.channels();
        ContentItem created = contentService.createContent(storeId, request.title(), request.body(), channels);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/{contentId}/retry")
    public ContentItem retry(@PathVariable UUID storeId, @PathVariable UUID contentId) {
        return contentService.retryContent(storeId, contentId);
    }

    @PatchMapping("/{contentId}/status")
    public ContentItem changeStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID contentId,
            @RequestBody ChangeContentStatusRequest request
    ) {
        return contentService.updateStatus(storeId, contentId, request.status());
    }

    public record CreateContentRequest(
            String title,
            String body,
            List<String> channels,
            Boolean aiSuggest,
            String eventText
    ) {
    }

    public record ChangeContentStatusRequest(
            ContentStatus status
    ) {
    }
}
