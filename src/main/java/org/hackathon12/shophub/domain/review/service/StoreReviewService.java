package org.hackathon12.shophub.domain.review.service;

import org.hackathon12.shophub.domain.ai.model.AiGeneratedText;
import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;
import org.hackathon12.shophub.domain.ai.service.AiTextGenerationService;
import org.hackathon12.shophub.domain.review.model.ReviewInbox;
import org.hackathon12.shophub.domain.review.model.ReviewPlatform;
import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.model.StoreReviewSummary;
import org.hackathon12.shophub.domain.review.port.ReviewReplyPublisherPort;
import org.hackathon12.shophub.domain.review.port.StoreReviewPort;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.infrastructure.mockmap.MockMapOwnerOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StoreReviewService {

    private static final Logger log = LoggerFactory.getLogger(StoreReviewService.class);
    private static final String MOCK_MAP_PLATFORM = "MOCK_MAP";

    private final StoreReviewPort storeReviewPort;
    private final ReviewReplyPublisherPort reviewReplyPublisherPort;
    private final ReviewInboxService reviewInboxService;
    private final StoreProfileService storeProfileService;
    private final AiTextGenerationService aiTextGenerationService;
    private final MockMapOwnerOAuthService mockMapOwnerOAuthService;

    public StoreReviewService(
            StoreReviewPort storeReviewPort,
            ReviewReplyPublisherPort reviewReplyPublisherPort,
            ReviewInboxService reviewInboxService,
            StoreProfileService storeProfileService,
            AiTextGenerationService aiTextGenerationService,
            @Lazy MockMapOwnerOAuthService mockMapOwnerOAuthService
    ) {
        this.storeReviewPort = storeReviewPort;
        this.reviewReplyPublisherPort = reviewReplyPublisherPort;
        this.reviewInboxService = reviewInboxService;
        this.storeProfileService = storeProfileService;
        this.aiTextGenerationService = aiTextGenerationService;
        this.mockMapOwnerOAuthService = mockMapOwnerOAuthService;
    }

    public List<StoreReview> getReviews(UUID storeId, String keyword) {
        storeProfileService.getStore(storeId);
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
        List<StoreReview> syncedReviews = storeReviewPort.findByStoreId(storeId);
        int syncedCount = syncedReviews.size();
        String platform = syncedReviews.stream()
                .findFirst()
                .map(StoreReview::platform)
                .orElse(MOCK_MAP_PLATFORM);
        return new StoreReviewSummary(
                platform,
                storeProfile.googleTotalReviews(),
                syncedCount,
                storeProfile.googleReviewUrl()
        );
    }

    public List<StoreReview> syncMockMapReviewsAndList(UUID storeId) {
        StoreReviewPort.MergeResult mergeResult = pullMockMapReviews(storeId);
        log.info(
                "MockMap review manual sync completed: storeId={}, newReviews={}, updatedReviews={}",
                storeId,
                mergeResult.newReviews(),
                mergeResult.updatedReviews()
        );
        return getReviews(storeId, null);
    }

    public MockMapReviewSyncResult syncMockMapReviews(UUID storeId) {
        if (mockMapOwnerOAuthService.findConnectedPlaceId(storeId).isEmpty()) {
            return MockMapReviewSyncResult.skipped();
        }

        try {
            StoreReviewPort.MergeResult mergeResult = pullMockMapReviews(storeId);
            return new MockMapReviewSyncResult(
                    1,
                    mergeResult.newReviews(),
                    mergeResult.updatedReviews(),
                    null
            );
        } catch (RuntimeException exception) {
            log.warn("MockMap review sync failed for storeId={}: {}", storeId, exception.getMessage());
            return new MockMapReviewSyncResult(1, 0, 0, exception.getMessage());
        }
    }

    @Transactional
    public void clearSyncedReviews(UUID storeId) {
        storeReviewPort.replaceByStoreId(storeId, List.of());
    }

    private StoreReviewPort.MergeResult pullMockMapReviews(UUID storeId) {
        storeProfileService.getStore(storeId);
        String placeId = mockMapOwnerOAuthService.findConnectedPlaceId(storeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MockMap OAuth가 연결되지 않았습니다. 가게 정보에서 MAP을 연동해 주세요."
                ));

        ReviewInbox reviewInbox = reviewInboxService.getUnifiedInbox(List.of(placeId));
        List<StoreReview> incoming = reviewInbox.reviews().stream()
                .map(review -> new StoreReview(
                        UUID.randomUUID(),
                        storeId,
                        ReviewPlatform.fromValue(review.sourcePlatform()).name(),
                        review.sourceReviewId(),
                        review.authorName(),
                        review.rating(),
                        review.content(),
                        review.reviewedAt(),
                        null,
                        null
                ))
                .toList();

        StoreReviewPort.MergeResult mergeResult = storeReviewPort.mergeFromSource(storeId, incoming);
        storeProfileService.updateGoogleReviewMeta(storeId, reviewInbox.summary().totalReviews());
        return mergeResult;
    }

    public MockMapReviewSyncResult syncAllMockMapReviews() {
        List<UUID> connectedStoreIds = mockMapOwnerOAuthService.findConnectedStoreIds();

        int storesProcessed = 0;
        int newReviews = 0;
        int updatedReviews = 0;

        for (UUID storeId : connectedStoreIds) {
            MockMapReviewSyncResult result = syncMockMapReviews(storeId);
            if (result.error() != null) {
                continue;
            }
            storesProcessed++;
            newReviews += result.newReviews();
            updatedReviews += result.updatedReviews();
        }

        return new MockMapReviewSyncResult(storesProcessed, newReviews, updatedReviews, null);
    }

    public record MockMapReviewSyncResult(
            int storesProcessed,
            int newReviews,
            int updatedReviews,
            String error
    ) {
        public static MockMapReviewSyncResult skipped() {
            return new MockMapReviewSyncResult(0, 0, 0, null);
        }
    }

    public AiGeneratedText suggestReviewReply(UUID reviewId) {
        StoreReview review = requireReview(reviewId);
        ensureReviewHasNoReply(review);

        StoreProfile storeProfile = storeProfileService.getStore(review.storeId());
        return aiTextGenerationService.suggestReviewReply(new ReviewReplyPrompt(
                storeProfile.name(),
                review.content(),
                review.rating()
        ));
    }

    @Transactional
    public StoreReview reply(UUID reviewId, String replyContent) {
        StoreReview review = requireReview(reviewId);
        if (!StringUtils.hasText(replyContent)) {
            throw new IllegalArgumentException("답글 내용은 필수입니다.");
        }
        ensureReviewHasNoReply(review);

        String normalizedReply = replyContent.trim();
        StoreReview replied = new StoreReview(
                review.id(),
                review.storeId(),
                review.platform(),
                review.sourceReviewId(),
                review.authorName(),
                review.rating(),
                review.content(),
                review.reviewedAt(),
                normalizedReply,
                Instant.now()
        );
        StoreReview saved = storeReviewPort.save(replied);
        reviewReplyPublisherPort.publishReply(saved, normalizedReply);
        return saved;
    }

    @Transactional
    public StoreReview deleteReply(UUID reviewId) {
        StoreReview review = requireReview(reviewId);
        ensureReviewHasReply(review);

        StoreReview cleared = new StoreReview(
                review.id(),
                review.storeId(),
                review.platform(),
                review.sourceReviewId(),
                review.authorName(),
                review.rating(),
                review.content(),
                review.reviewedAt(),
                null,
                null
        );
        StoreReview saved = storeReviewPort.save(cleared);
        reviewReplyPublisherPort.deleteReply(review);
        return saved;
    }

    private StoreReview requireReview(UUID reviewId) {
        StoreReview review = storeReviewPort.findById(reviewId);
        if (review == null) {
            throw new NotFoundException("리뷰를 찾을 수 없습니다. reviewId=" + reviewId);
        }
        return review;
    }

    private void ensureReviewHasNoReply(StoreReview review) {
        if (StringUtils.hasText(review.reply())) {
            throw new IllegalArgumentException("이미 답글이 등록된 리뷰입니다.");
        }
    }

    private void ensureReviewHasReply(StoreReview review) {
        if (!StringUtils.hasText(review.reply())) {
            throw new IllegalArgumentException("등록된 답글이 없습니다.");
        }
    }

    public StoreReview autoReplyWithAi(UUID reviewId) {
        AiGeneratedText suggestion = suggestReviewReply(reviewId);
        return reply(reviewId, suggestion.text());
    }
}
