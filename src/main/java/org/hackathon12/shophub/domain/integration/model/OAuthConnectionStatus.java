package org.hackathon12.shophub.domain.integration.model;

public record OAuthConnectionStatus(
        OAuthIntegrationType type,
        boolean credentialsConfigured,
        boolean connected,
        String clientId,
        String placeId,
        String placeName,
        String updatedAt
) {
}
