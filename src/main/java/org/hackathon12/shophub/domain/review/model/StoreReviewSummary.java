package org.hackathon12.shophub.domain.review.model;

public record StoreReviewSummary(
        String platform,
        int totalReviewCount,
        int syncedReviewCount,
        String externalUrl
) {
}
