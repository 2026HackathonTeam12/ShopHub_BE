package org.hackathon12.shophub.domain.review.model;

import java.time.Instant;
import java.util.UUID;

public record StoreReview(
        UUID id,
        UUID storeId,
        String platform,
        String sourceReviewId,
        String authorName,
        int rating,
        String content,
        Instant reviewedAt,
        String reply,
        Instant repliedAt
) {
}
