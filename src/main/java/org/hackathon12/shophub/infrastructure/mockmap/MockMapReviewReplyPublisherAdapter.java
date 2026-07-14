package org.hackathon12.shophub.infrastructure.mockmap;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.ReviewReplyPublisherPort;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@Primary
@Profile("!test")
@ConditionalOnProperty(prefix = "app.review-source", name = "provider", havingValue = "mockmap", matchIfMissing = true)
public class MockMapReviewReplyPublisherAdapter implements ReviewReplyPublisherPort {

    private static final String MOCK_MAP_PLATFORM = "MOCK_MAP";

    private final MockMapOwnerOAuthService mockMapOwnerOAuthService;
    private final MockMapApiClient mockMapApiClient;
    private final StoreProfileService storeProfileService;

    public MockMapReviewReplyPublisherAdapter(
            MockMapOwnerOAuthService mockMapOwnerOAuthService,
            MockMapApiClient mockMapApiClient,
            StoreProfileService storeProfileService
    ) {
        this.mockMapOwnerOAuthService = mockMapOwnerOAuthService;
        this.mockMapApiClient = mockMapApiClient;
        this.storeProfileService = storeProfileService;
    }

    @Override
    public void publishReply(StoreReview review, String replyContent) {
        if (!MOCK_MAP_PLATFORM.equalsIgnoreCase(review.platform())) {
            return;
        }
        if (!StringUtils.hasText(review.sourceReviewId())) {
            throw new MockMapApiException(
                    "MockMap 원본 리뷰 ID가 없어 답글을 등록할 수 없습니다. reviewId=" + review.id()
            );
        }

        String accessToken = mockMapOwnerOAuthService.getAccessToken(review.storeId());
        mockMapApiClient.createReply(review.sourceReviewId(), accessToken, replyContent);
    }

    @Override
    public void deleteReply(StoreReview review) {
        if (!MOCK_MAP_PLATFORM.equalsIgnoreCase(review.platform())) {
            return;
        }
        if (!StringUtils.hasText(review.sourceReviewId())) {
            return;
        }

        StoreProfile storeProfile = storeProfileService.getStore(review.storeId());
        if (!StringUtils.hasText(storeProfile.googlePlaceId())) {
            return;
        }

        String accessToken = mockMapOwnerOAuthService.getAccessToken(review.storeId());
        String replyId = findFirstReplyId(storeProfile.googlePlaceId(), review.sourceReviewId());
        if (!StringUtils.hasText(replyId)) {
            return;
        }

        mockMapApiClient.deleteReply(review.sourceReviewId(), replyId, accessToken);
    }

    private String findFirstReplyId(String placeId, String sourceReviewId) {
        MockMapReviewCollectionResponse response = mockMapApiClient.listReviews(placeId);
        if (response == null || response.data() == null) {
            return null;
        }

        for (MockMapReviewCollectionResponse.ReviewItem reviewItem : response.data()) {
            if (reviewItem.id() == null || !sourceReviewId.equals(String.valueOf(reviewItem.id()))) {
                continue;
            }
            List<MockMapReviewCollectionResponse.ReplyItem> replies = reviewItem.replies();
            if (replies == null || replies.isEmpty() || replies.getFirst().id() == null) {
                return null;
            }
            return String.valueOf(replies.getFirst().id());
        }
        return null;
    }
}
