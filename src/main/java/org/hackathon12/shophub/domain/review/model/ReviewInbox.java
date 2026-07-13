package org.hackathon12.shophub.domain.review.model;

import java.time.Instant;
import java.util.List;

public record ReviewInbox(
        Instant generatedAt,
        ReviewSummary summary,
        List<SourceSnapshot> sources,
        List<UnifiedReview> reviews
) {

    public record ReviewSummary(
            int totalReviews,
            double averageRating
    ) {
    }

    public record SourceSnapshot(
            String placeId,
            String storeName,
            Double placeRating,
            Integer placeRatingCount,
            String placeUrl
    ) {
    }

    public record UnifiedReview(
            String sourcePlatform,
            String placeId,
            String storeName,
            String authorName,
            int rating,
            String content,
            Instant reviewedAt
    ) {
    }
}
