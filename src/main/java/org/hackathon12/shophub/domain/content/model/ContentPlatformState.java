package org.hackathon12.shophub.domain.content.model;

public record ContentPlatformState(
        ContentChannel platform,
        ContentChannelPublishStatus status
) {
}
