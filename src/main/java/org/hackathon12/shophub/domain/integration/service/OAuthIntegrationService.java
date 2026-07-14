package org.hackathon12.shophub.domain.integration.service;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OAuthIntegrationService {

    private final Map<OAuthIntegrationType, OAuthIntegrationProvider> providers;

    public OAuthIntegrationService(List<OAuthIntegrationProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        OAuthIntegrationProvider::type,
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException(
                                    "OAuthIntegrationProvider type 중복: " + left.type()
                            );
                        }
                ));
    }

    public OAuthIntegrationProvider requireProvider(OAuthIntegrationType type) {
        OAuthIntegrationProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("지원하지 않는 OAuth 연동 type입니다: " + type.pathValue());
        }
        return provider;
    }

    public OAuthIntegrationProvider requireProvider(String typePathValue) {
        return requireProvider(OAuthIntegrationType.fromPathValue(typePathValue));
    }

    public List<OAuthConnectionStatus> listConnectionStatuses(UUID storeId) {
        return providers.values().stream()
                .map(provider -> provider.getConnectionStatus(storeId))
                .filter(status -> status.credentialsConfigured() || status.connected())
                .sorted(Comparator.comparing(status -> status.type().pathValue()))
                .toList();
    }
}
