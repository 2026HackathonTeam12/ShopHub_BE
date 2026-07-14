package org.hackathon12.shophub.infrastructure.review;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.ReviewReplyPublisherPort;
import org.springframework.stereotype.Component;

@Component
public class NoOpReviewReplyPublisherAdapter implements ReviewReplyPublisherPort {

    @Override
    public void publishReply(StoreReview review, String replyContent) {
        // Intentionally no-op when external reply publishing is disabled.
    }

    @Override
    public void deleteReply(StoreReview review) {
        // Intentionally no-op when external reply publishing is disabled.
    }
}
