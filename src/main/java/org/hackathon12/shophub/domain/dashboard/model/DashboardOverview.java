package org.hackathon12.shophub.domain.dashboard.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardOverview(
        UUID storeId,
        String storeName,
        DraftContent draftContent,
        SuggestionCard suggestionCard,
        ReviewWidget reviewWidget,
        List<ChecklistItem> checklistItems,
        List<RecentReviewItem> recentReviews,
        Instant generatedAt
) {

    public record DraftContent(
            String title,
            String body,
            int charCount,
            int maxCharCount,
            List<String> channels
    ) {
    }

    public record SuggestionCard(
            String title,
            String message,
            String actionLabel
    ) {
    }

    public record ReviewWidget(
            int totalReviews,
            int syncedRecentReviews,
            String reviewListUrl
    ) {
    }

    public record ChecklistItem(
            String key,
            String title,
            boolean completed
    ) {
    }

    public record RecentReviewItem(
            UUID reviewId,
            String authorName,
            int rating,
            String content,
            String ageLabel
    ) {
    }
}
