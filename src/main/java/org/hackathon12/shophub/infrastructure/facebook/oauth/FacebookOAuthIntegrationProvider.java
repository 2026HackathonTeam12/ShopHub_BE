package org.hackathon12.shophub.infrastructure.facebook.oauth;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FacebookOAuthIntegrationProvider implements OAuthIntegrationProvider {

    private final FacebookOwnerOAuthService facebookOwnerOAuthService;

    public FacebookOAuthIntegrationProvider(FacebookOwnerOAuthService facebookOwnerOAuthService) {
        this.facebookOwnerOAuthService = facebookOwnerOAuthService;
    }

    @Override
    public OAuthIntegrationType type() {
        return OAuthIntegrationType.FACEBOOK;
    }

    @Override
    public OAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        return toConnectionStatus(facebookOwnerOAuthService.saveCredentials(storeId, userId, clientId, clientSecret));
    }

    @Override
    public OAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        return toConnectionStatus(facebookOwnerOAuthService.deleteCredentials(storeId, userId));
    }

    @Override
    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        return facebookOwnerOAuthService.buildAuthorizationUrl(storeId, userId);
    }

    @Override
    public OAuthConnectionStatus completeAuthorization(String code, String state) {
        return toConnectionStatus(facebookOwnerOAuthService.completeAuthorization(code, state));
    }

    @Override
    public OAuthConnectionStatus getConnectionStatus(UUID storeId) {
        return toConnectionStatus(facebookOwnerOAuthService.getConnectionStatus(storeId));
    }

    @Override
    public OAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        return toConnectionStatus(facebookOwnerOAuthService.disconnect(storeId, userId));
    }

    private OAuthConnectionStatus toConnectionStatus(FacebookOAuthConnectionStatus status) {
        return new OAuthConnectionStatus(
                OAuthIntegrationType.FACEBOOK,
                status.credentialsConfigured(),
                status.connected(),
                status.clientId(),
                status.pageId(),
                status.pageName(),
                status.updatedAt()
        );
    }
}
