package org.hackathon12.shophub.infrastructure.mockmap;

import java.util.UUID;

public record MockMapOAuthPendingState(
        UUID storeId,
        UUID userId
) {
}
