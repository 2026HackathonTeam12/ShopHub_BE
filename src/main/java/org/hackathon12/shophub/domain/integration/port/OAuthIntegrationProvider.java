package org.hackathon12.shophub.domain.integration.port;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;

import java.util.UUID;

public interface OAuthIntegrationProvider {

    OAuthIntegrationType type();

    OAuthConnectionStatus saveCredentials(UUID storeId, UUID userId, String clientId, String clientSecret);

    OAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId);

    String buildAuthorizationUrl(UUID storeId, UUID userId);

    OAuthConnectionStatus completeAuthorization(String code, String state);

    OAuthConnectionStatus getConnectionStatus(UUID storeId);

    OAuthConnectionStatus disconnect(UUID storeId, UUID userId);
}
