package org.hackathon12.shophub.domain.review.model;

public record StoreReviewSummary(
        String platform,
        int externalTotalReviewCount,
        int localSyncedReviewCount,
        String externalUrl
) {
}
