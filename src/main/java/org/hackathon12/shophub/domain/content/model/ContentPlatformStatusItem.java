package org.hackathon12.shophub.domain.content.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentPlatformStatusItem(
        UUID id,
        UUID storeId,
        String title,
        String body,
        List<ContentPlatformState> platforms,
        Instant updatedAt
) {
}
