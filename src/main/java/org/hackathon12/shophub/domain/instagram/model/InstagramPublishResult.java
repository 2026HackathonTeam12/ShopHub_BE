package org.hackathon12.shophub.domain.instagram.model;

import java.time.Instant;
import java.util.List;

public record InstagramPublishResult(
        String mediaId,
        String caption,
        List<String> imageUrls,
        Instant publishedAt
) {
}
