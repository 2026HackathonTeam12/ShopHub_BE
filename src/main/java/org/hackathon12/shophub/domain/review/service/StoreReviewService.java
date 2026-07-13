package org.hackathon12.shophub.domain.review.service;

import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;
import org.hackathon12.shophub.domain.ai.service.AiTextGenerationService;
import org.hackathon12.shophub.domain.review.model.ReviewInbox;
import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.model.StoreReviewSummary;
import org.hackathon12.shophub.domain.review.port.StoreReviewPort;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StoreReviewService {

    private final StoreReviewPort storeReviewPort;
    private final ReviewInboxService reviewInboxService;
    private final StoreProfileService storeProfileService;
    private final AiTextGenerationService aiTextGenerationService;

    public StoreReviewService(
            StoreReviewPort storeReviewPort,
            ReviewInboxService reviewInboxService,
            StoreProfileService storeProfileService,
            AiTextGenerationService aiTextGenerationService
    ) {
        this.storeReviewPort = storeReviewPort;
        this.reviewInboxService = reviewInboxService;
        this.storeProfileService = storeProfileService;
        this.aiTextGenerationService = aiTextGenerationService;
    }

    public List<StoreReview> getReviews(UUID storeId, String keyword) {
        List<StoreReview> reviews = storeReviewPort.findByStoreId(storeId).stream()
                .sorted(Comparator.comparing(StoreReview::reviewedAt).reversed())
                .toList();

        if (keyword == null || keyword.isBlank()) {
            return reviews;
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        return reviews.stream()
                .filter(review -> review.content().toLowerCase().contains(normalizedKeyword)
                        || review.authorName().toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    public StoreReviewSummary getSummary(UUID storeId) {
        StoreProfile storeProfile = storeProfileService.getStore(storeId);
        int syncedCount = storeReviewPort.findByStoreId(storeId).size();
        return new StoreReviewSummary(
                "GOOGLE",
                storeProfile.googleTotalReviews(),
                syncedCount,
                storeProfile.googleReviewUrl()
        );
    }

    public List<StoreReview> syncGoogleReviews(UUID storeId, int limit) {
        StoreProfile storeProfile = storeProfileService.getStore(storeId);
        if (storeProfile.googlePlaceId() == null || storeProfile.googlePlaceId().isBlank()) {
            throw new IllegalArgumentException("Google Place ID가 등록되지 않았습니다.");
        }

        ReviewInbox reviewInbox = reviewInboxService.getUnifiedInbox(List.of(storeProfile.googlePlaceId()));
        List<StoreReview> synced = reviewInbox.reviews().stream()
                .limit(limit)
                .map(review -> new StoreReview(
                        UUID.randomUUID(),
                        storeId,
                        review.sourcePlatform(),
                        review.authorName(),
                        review.rating(),
                        review.content(),
                        review.reviewedAt(),
                        null,
                        null
                ))
                .toList();

        storeReviewPort.replaceByStoreId(storeId, synced);
        storeProfileService.updateGoogleReviewMeta(storeId, reviewInbox.summary().totalReviews());
        return synced;
    }

    public String generateAiDraft(UUID reviewId) {
        StoreReview review = storeReviewPort.findById(reviewId);
        if (review == null) {
            throw new NotFoundException("리뷰를 찾을 수 없습니다. reviewId=" + reviewId);
        }

        StoreProfile storeProfile = storeProfileService.getStore(review.storeId());
        return aiTextGenerationService.generateReviewReply(new ReviewReplyPrompt(
                storeProfile.name(),
                review.content(),
                review.rating()
        ));
    }

    public StoreReview reply(UUID reviewId, String replyContent) {
        StoreReview review = storeReviewPort.findById(reviewId);
        if (review == null) {
            throw new NotFoundException("리뷰를 찾을 수 없습니다. reviewId=" + reviewId);
        }

        StoreReview replied = new StoreReview(
                review.id(),
                review.storeId(),
                review.platform(),
                review.authorName(),
                review.rating(),
                review.content(),
                review.reviewedAt(),
                replyContent,
                Instant.now()
        );
        return storeReviewPort.save(replied);
    }

    public StoreReview autoReplyWithAi(UUID reviewId) {
        String aiDraft = generateAiDraft(reviewId);
        return reply(reviewId, aiDraft);
    }
}
