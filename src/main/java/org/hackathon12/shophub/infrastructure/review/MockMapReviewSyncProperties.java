package org.hackathon12.shophub.infrastructure.review;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.review-sync")
public record MockMapReviewSyncProperties(
        boolean enabled,
        String cron
) {
    public MockMapReviewSyncProperties {
        if (cron == null || cron.isBlank()) {
            cron = "0 */5 * * * *";
        }
    }
}
