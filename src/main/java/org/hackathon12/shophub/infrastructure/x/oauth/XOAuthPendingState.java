package org.hackathon12.shophub.infrastructure.x.oauth;

import java.util.UUID;

public record XOAuthPendingState(
        UUID storeId,
        UUID userId,
        String codeVerifier
) {
}
