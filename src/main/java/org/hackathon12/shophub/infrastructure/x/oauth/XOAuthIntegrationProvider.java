package org.hackathon12.shophub.infrastructure.x.oauth;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class XOAuthIntegrationProvider implements OAuthIntegrationProvider {

    private final XOwnerOAuthService xOwnerOAuthService;

    public XOAuthIntegrationProvider(XOwnerOAuthService xOwnerOAuthService) {
        this.xOwnerOAuthService = xOwnerOAuthService;
    }

    @Override
    public OAuthIntegrationType type() {
        return OAuthIntegrationType.X;
    }

    @Override
    public OAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        return toConnectionStatus(xOwnerOAuthService.saveCredentials(storeId, userId, clientId, clientSecret));
    }

    @Override
    public OAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        return toConnectionStatus(xOwnerOAuthService.deleteCredentials(storeId, userId));
    }

    @Override
    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        return xOwnerOAuthService.buildAuthorizationUrl(storeId, userId);
    }

    @Override
    public OAuthConnectionStatus completeAuthorization(String code, String state) {
        return toConnectionStatus(xOwnerOAuthService.completeAuthorization(code, state));
    }

    @Override
    public OAuthConnectionStatus getConnectionStatus(UUID storeId) {
        return toConnectionStatus(xOwnerOAuthService.getConnectionStatus(storeId));
    }

    @Override
    public OAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        return toConnectionStatus(xOwnerOAuthService.disconnect(storeId, userId));
    }

    private OAuthConnectionStatus toConnectionStatus(XOAuthConnectionStatus status) {
        return new OAuthConnectionStatus(
                OAuthIntegrationType.X,
                status.credentialsConfigured(),
                status.connected(),
                status.clientId(),
                status.xUserId(),
                status.xUsername(),
                status.updatedAt()
        );
    }
}
