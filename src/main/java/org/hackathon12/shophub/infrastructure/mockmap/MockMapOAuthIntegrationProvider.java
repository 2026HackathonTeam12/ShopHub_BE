package org.hackathon12.shophub.infrastructure.mockmap;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.review-source", name = "provider", havingValue = "mockmap", matchIfMissing = true)
public class MockMapOAuthIntegrationProvider implements OAuthIntegrationProvider {

    private final MockMapOwnerOAuthService mockMapOwnerOAuthService;

    public MockMapOAuthIntegrationProvider(MockMapOwnerOAuthService mockMapOwnerOAuthService) {
        this.mockMapOwnerOAuthService = mockMapOwnerOAuthService;
    }

    @Override
    public OAuthIntegrationType type() {
        return OAuthIntegrationType.MOCK_MAP;
    }

    @Override
    public OAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        return toConnectionStatus(mockMapOwnerOAuthService.saveCredentials(storeId, userId, clientId, clientSecret));
    }

    @Override
    public OAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        return toConnectionStatus(mockMapOwnerOAuthService.deleteCredentials(storeId, userId));
    }

    @Override
    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        return mockMapOwnerOAuthService.buildAuthorizationUrl(storeId, userId);
    }

    @Override
    public OAuthConnectionStatus completeAuthorization(String code, String state) {
        return toConnectionStatus(mockMapOwnerOAuthService.completeAuthorization(code, state));
    }

    @Override
    public OAuthConnectionStatus getConnectionStatus(UUID storeId) {
        return toConnectionStatus(mockMapOwnerOAuthService.getConnectionStatus(storeId));
    }

    @Override
    public OAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        return toConnectionStatus(mockMapOwnerOAuthService.disconnect(storeId, userId));
    }

    private OAuthConnectionStatus toConnectionStatus(MockMapOAuthConnectionStatus status) {
        return new OAuthConnectionStatus(
                OAuthIntegrationType.MOCK_MAP,
                status.credentialsConfigured(),
                status.connected(),
                status.clientId(),
                status.placeId(),
                status.placeName(),
                status.updatedAt()
        );
    }
}
