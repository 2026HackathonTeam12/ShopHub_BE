package org.hackathon12.shophub.infrastructure.web.review;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.model.StoreReviewSummary;
import org.hackathon12.shophub.domain.review.service.StoreReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class StoreReviewController {

    private final StoreReviewService storeReviewService;

    public StoreReviewController(StoreReviewService storeReviewService) {
        this.storeReviewService = storeReviewService;
    }

    @GetMapping("/v1/stores/{storeId}/reviews/summary")
    public StoreReviewSummary getSummary(@PathVariable UUID storeId) {
        return storeReviewService.getSummary(storeId);
    }

    @GetMapping("/v1/stores/{storeId}/reviews")
    public List<StoreReview> getReviews(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword
    ) {
        return storeReviewService.getReviews(storeId, keyword);
    }

    @PostMapping("/v1/stores/{storeId}/reviews/sync-google")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<StoreReview> syncGoogleReviews(
            @PathVariable UUID storeId,
            @RequestBody(required = false) SyncGoogleReviewsRequest request
    ) {
        int limit = request == null || request.limit() == null ? 5 : request.limit();
        return storeReviewService.syncGoogleReviews(storeId, limit);
    }

    @PostMapping("/v1/reviews/{reviewId}/ai-draft")
    public AiDraftResponse createAiDraft(@PathVariable UUID reviewId) {
        return new AiDraftResponse(storeReviewService.generateAiDraft(reviewId));
    }

    @PostMapping("/v1/reviews/{reviewId}/ai-auto-reply")
    public StoreReview createAndSaveAiReply(@PathVariable UUID reviewId) {
        return storeReviewService.autoReplyWithAi(reviewId);
    }

    @PostMapping("/v1/reviews/{reviewId}/reply")
    public StoreReview reply(@PathVariable UUID reviewId, @RequestBody ReplyRequest request) {
        return storeReviewService.reply(reviewId, request.content());
    }

    public record SyncGoogleReviewsRequest(
            Integer limit
    ) {
    }

    public record AiDraftResponse(
            String content
    ) {
    }

    public record ReplyRequest(
            String content
    ) {
    }
}
