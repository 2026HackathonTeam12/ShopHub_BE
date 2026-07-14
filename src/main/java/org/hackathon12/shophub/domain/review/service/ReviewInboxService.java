package org.hackathon12.shophub.domain.review.service;

import org.hackathon12.shophub.domain.review.model.ReviewInbox;
import org.hackathon12.shophub.domain.review.model.ReviewSourceData;
import org.hackathon12.shophub.domain.review.port.ReviewSourcePort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReviewInboxService {

    private final ReviewSourcePort reviewSourcePort;

    public ReviewInboxService(ReviewSourcePort reviewSourcePort) {
        this.reviewSourcePort = reviewSourcePort;
    }

    public ReviewInbox getUnifiedInbox(List<String> placeIds) {
        List<ReviewInbox.SourceSnapshot> sourceSnapshots = new ArrayList<>();
        List<ReviewInbox.UnifiedReview> unifiedReviews = new ArrayList<>();

        for (String placeId : placeIds) {
            ReviewSourceData sourceData = reviewSourcePort.fetchByPlaceId(placeId);

            sourceSnapshots.add(new ReviewInbox.SourceSnapshot(
                    sourceData.placeId(),
                    sourceData.storeName(),
                    sourceData.placeRating(),
                    sourceData.placeRatingCount(),
                    sourceData.placeUrl()
            ));

            for (ReviewSourceData.ReviewItem review : sourceData.reviews()) {
                unifiedReviews.add(new ReviewInbox.UnifiedReview(
                        sourceData.sourcePlatform(),
                        sourceData.placeId(),
                        sourceData.storeName(),
                        review.sourceReviewId(),
                        review.authorName(),
                        review.rating(),
                        review.content(),
                        review.reviewedAt()
                ));
            }
        }

        unifiedReviews.sort(Comparator.comparing(ReviewInbox.UnifiedReview::reviewedAt).reversed());
        ReviewInbox.ReviewSummary summary = createSummary(unifiedReviews);

        return new ReviewInbox(
                Instant.now(),
                summary,
                sourceSnapshots,
                unifiedReviews
        );
    }

    private ReviewInbox.ReviewSummary createSummary(List<ReviewInbox.UnifiedReview> reviews) {
        if (reviews.isEmpty()) {
            return new ReviewInbox.ReviewSummary(0, 0.0);
        }

        int totalReviews = reviews.size();
        double totalRating = reviews.stream()
                .mapToInt(ReviewInbox.UnifiedReview::rating)
                .sum();

        return new ReviewInbox.ReviewSummary(totalReviews, totalRating / totalReviews);
    }
}
