package org.hackathon12.shophub.infrastructure.mockmap;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.ReviewReplyPublisherPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Primary
@Profile("!test")
@ConditionalOnProperty(prefix = "app.review-source", name = "provider", havingValue = "mockmap", matchIfMissing = true)
public class MockMapReviewReplyPublisherAdapter implements ReviewReplyPublisherPort {

    private static final String MOCK_MAP_PLATFORM = "MOCK_MAP";

    private final MockMapOwnerOAuthService mockMapOwnerOAuthService;
    private final MockMapApiClient mockMapApiClient;

    public MockMapReviewReplyPublisherAdapter(
            MockMapOwnerOAuthService mockMapOwnerOAuthService,
            MockMapApiClient mockMapApiClient
    ) {
        this.mockMapOwnerOAuthService = mockMapOwnerOAuthService;
        this.mockMapApiClient = mockMapApiClient;
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
}
