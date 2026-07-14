package org.hackathon12.shophub.infrastructure.review;

import org.hackathon12.shophub.domain.review.service.StoreReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.review-source", name = "provider", havingValue = "mockmap", matchIfMissing = true)
public class MockMapReviewSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MockMapReviewSyncScheduler.class);

    private final StoreReviewService storeReviewService;
    private final MockMapReviewSyncProperties properties;

    public MockMapReviewSyncScheduler(
            StoreReviewService storeReviewService,
            MockMapReviewSyncProperties properties
    ) {
        this.storeReviewService = storeReviewService;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.review-sync.cron:0 */5 * * * *}")
    public void syncReviewsFromMockMap() {
        if (!properties.enabled()) {
            return;
        }

        try {
            StoreReviewService.MockMapReviewSyncResult result = storeReviewService.syncAllMockMapReviews();
            if (result.storesProcessed() == 0) {
                log.debug("MockMap review sync skipped: no stores with place_id");
                return;
            }
            log.info(
                    "MockMap review sync completed: stores={}, newReviews={}, updatedReviews={}",
                    result.storesProcessed(),
                    result.newReviews(),
                    result.updatedReviews()
            );
        } catch (Exception exception) {
            log.error("MockMap review sync failed: {}", exception.getMessage(), exception);
        }
    }
}
