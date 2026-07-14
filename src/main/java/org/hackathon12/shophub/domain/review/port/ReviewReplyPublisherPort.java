package org.hackathon12.shophub.domain.review.port;

import org.hackathon12.shophub.domain.review.model.StoreReview;

public interface ReviewReplyPublisherPort {

    void publishReply(StoreReview review, String replyContent);

    void deleteReply(StoreReview review);
}
