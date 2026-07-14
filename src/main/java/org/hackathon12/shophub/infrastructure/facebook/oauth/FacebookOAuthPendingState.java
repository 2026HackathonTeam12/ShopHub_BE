package org.hackathon12.shophub.infrastructure.facebook.oauth;

import java.util.UUID;

public record FacebookOAuthPendingState(
        UUID storeId,
        UUID userId
) {
}
