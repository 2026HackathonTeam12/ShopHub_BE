package org.hackathon12.shophub.infrastructure.mockmap;

public record MockMapOAuthConnectionStatus(
        boolean credentialsConfigured,
        boolean connected,
        String clientId,
        String placeId,
        String placeName,
        String updatedAt
) {
}
