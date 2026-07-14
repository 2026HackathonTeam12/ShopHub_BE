package org.hackathon12.shophub.infrastructure.web.review;

import org.hackathon12.shophub.domain.ai.model.AiGenerationSource;
import org.hackathon12.shophub.domain.ai.model.AiGeneratedText;
import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.model.StoreReviewSummary;
import org.hackathon12.shophub.domain.review.service.StoreReviewService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class StoreReviewController {

    private final StoreReviewService storeReviewService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public StoreReviewController(StoreReviewService storeReviewService, ShopHubAuthGuard shopHubAuthGuard) {
        this.storeReviewService = storeReviewService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @GetMapping("/v1/stores/{storeId}/reviews/summary")
    public StoreReviewSummary getSummary(@PathVariable UUID storeId, HttpServletRequest request) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeReviewService.getSummary(storeId);
    }

    @GetMapping("/v1/stores/{storeId}/reviews")
    public List<StoreReview> getReviews(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeReviewService.getReviews(storeId, keyword);
    }

    @PostMapping("/v1/stores/{storeId}/reviews/sync-mockmap")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<StoreReview> syncMockMapReviews(@PathVariable UUID storeId, HttpServletRequest request) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return storeReviewService.syncMockMapReviewsAndList(storeId);
    }

    @PostMapping("/v1/reviews/{reviewId}/ai-draft")
    public AiDraftResponse createAiDraft(@PathVariable UUID reviewId, HttpServletRequest request) {
        shopHubAuthGuard.requireReviewMember(request, reviewId);
        AiGeneratedText suggestion = storeReviewService.suggestReviewReply(reviewId);
        return new AiDraftResponse(suggestion.text(), suggestion.source());
    }

    @PostMapping("/v1/reviews/{reviewId}/ai-auto-reply")
    public StoreReview createAndSaveAiReply(@PathVariable UUID reviewId, HttpServletRequest request) {
        shopHubAuthGuard.requireReviewMember(request, reviewId);
        return storeReviewService.autoReplyWithAi(reviewId);
    }

    @PostMapping("/v1/reviews/{reviewId}/reply")
    public StoreReview reply(
            @PathVariable UUID reviewId,
            @RequestBody ReplyRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireReviewMember(request, reviewId);
        return storeReviewService.reply(reviewId, requestBody.content());
    }

    @DeleteMapping("/v1/reviews/{reviewId}/reply")
    public StoreReview deleteReply(@PathVariable UUID reviewId, HttpServletRequest request) {
        shopHubAuthGuard.requireReviewMember(request, reviewId);
        return storeReviewService.deleteReply(reviewId);
    }

    public record AiDraftResponse(
            String content,
            AiGenerationSource source
    ) {
    }

    public record ReplyRequest(
            String content
    ) {
    }
}
