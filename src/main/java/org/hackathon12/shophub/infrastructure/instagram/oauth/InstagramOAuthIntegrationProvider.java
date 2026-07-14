package org.hackathon12.shophub.infrastructure.instagram.oauth;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Instagram은 Meta Graph 공용 계정을 pseudo-OAuth로 매장에 연결합니다.
@Component
public class InstagramOAuthIntegrationProvider implements OAuthIntegrationProvider {

    private final InstagramOwnerOAuthService instagramOwnerOAuthService;

    public InstagramOAuthIntegrationProvider(InstagramOwnerOAuthService instagramOwnerOAuthService) {
        this.instagramOwnerOAuthService = instagramOwnerOAuthService;
    }

    @Override
    public OAuthIntegrationType type() {
        return OAuthIntegrationType.INSTAGRAM;
    }

    @Override
    public OAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        return toConnectionStatus(instagramOwnerOAuthService.saveCredentials(storeId, userId, clientId, clientSecret));
    }

    @Override
    public OAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        return toConnectionStatus(instagramOwnerOAuthService.deleteCredentials(storeId, userId));
    }

    @Override
    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        return instagramOwnerOAuthService.buildAuthorizationUrl(storeId, userId);
    }

    @Override
    public OAuthConnectionStatus completeAuthorization(String code, String state) {
        return toConnectionStatus(instagramOwnerOAuthService.completeAuthorization(code, state));
    }

    @Override
    public OAuthConnectionStatus getConnectionStatus(UUID storeId) {
        return toConnectionStatus(instagramOwnerOAuthService.getConnectionStatus(storeId));
    }

    @Override
    public OAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        return toConnectionStatus(instagramOwnerOAuthService.disconnect(storeId, userId));
    }

    private OAuthConnectionStatus toConnectionStatus(InstagramOAuthConnectionStatus status) {
        return new OAuthConnectionStatus(
                OAuthIntegrationType.INSTAGRAM,
                status.credentialsConfigured(),
                status.connected(),
                status.clientId(),
                status.accountId(),
                status.username(),
                status.updatedAt()
        );
    }
}
