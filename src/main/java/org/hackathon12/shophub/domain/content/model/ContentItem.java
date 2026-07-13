package org.hackathon12.shophub.domain.content.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentItem(
        UUID id,
        UUID storeId,
        String title,
        String body,
        List<String> channels,
        ContentStatus status,
        Instant updatedAt
) {
}
