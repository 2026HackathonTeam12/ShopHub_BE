package org.hackathon12.shophub.infrastructure.x.oauth;

public record XOAuthConnectionStatus(
        boolean credentialsConfigured,
        boolean connected,
        String clientId,
        String xUserId,
        String xUsername,
        String updatedAt
) {
}
