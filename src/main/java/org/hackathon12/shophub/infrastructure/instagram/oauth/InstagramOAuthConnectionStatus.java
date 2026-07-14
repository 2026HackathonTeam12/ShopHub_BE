package org.hackathon12.shophub.infrastructure.instagram.oauth;

public record InstagramOAuthConnectionStatus(
        boolean credentialsConfigured,
        boolean connected,
        String clientId,
        String accountId,
        String username,
        String updatedAt
) {
}
