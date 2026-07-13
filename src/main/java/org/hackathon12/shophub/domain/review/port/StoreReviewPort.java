package org.hackathon12.shophub.domain.review.port;

import org.hackathon12.shophub.domain.review.model.StoreReview;

import java.util.List;
import java.util.UUID;

public interface StoreReviewPort {

    List<StoreReview> findByStoreId(UUID storeId);

    StoreReview findById(UUID reviewId);

    void replaceByStoreId(UUID storeId, List<StoreReview> reviews);

    StoreReview save(StoreReview storeReview);
}
