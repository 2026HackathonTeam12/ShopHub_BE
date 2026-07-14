package org.hackathon12.shophub.infrastructure.facebook.oauth;

public record FacebookOAuthConnectionStatus(
        boolean credentialsConfigured,
        boolean connected,
        String clientId,
        String pageId,
        String pageName,
        String updatedAt
) {
}
