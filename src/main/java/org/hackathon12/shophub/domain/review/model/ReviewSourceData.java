package org.hackathon12.shophub.domain.review.model;

import java.time.Instant;
import java.util.List;

public record ReviewSourceData(
        String sourcePlatform,
        String placeId,
        String storeName,
        Double placeRating,
        Integer placeRatingCount,
        String placeUrl,
        List<ReviewItem> reviews
) {

    public record ReviewItem(
            String authorName,
            int rating,
            String content,
            Instant reviewedAt
    ) {
    }
}
