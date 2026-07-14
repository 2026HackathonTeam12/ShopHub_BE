package org.hackathon12.shophub.infrastructure.instagram.oauth;

import java.util.UUID;

public record InstagramOAuthPendingState(
        UUID storeId,
        UUID userId
) {
}
