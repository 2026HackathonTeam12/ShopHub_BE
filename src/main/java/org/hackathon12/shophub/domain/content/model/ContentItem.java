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
        Instant updatedAt,
        List<ContentPlatformState> platforms
) {
    public static List<ContentPlatformState> pendingPlatformsFor(List<String> channels) {
        return channels.stream()
                .map(ContentChannel::fromValue)
                .map(channel -> new ContentPlatformState(channel, ContentChannelPublishStatus.PENDING))
                .toList();
    }
}
