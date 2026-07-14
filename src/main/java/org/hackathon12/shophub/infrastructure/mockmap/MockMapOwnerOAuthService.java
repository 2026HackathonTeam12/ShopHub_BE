package org.hackathon12.shophub.infrastructure.mockmap;

import org.hackathon12.shophub.domain.review.service.StoreReviewService;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.infrastructure.persistence.StoreMockMapConnectionEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreMockMapConnectionJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountEntity;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MockMapOwnerOAuthService {

    private static final Logger log = LoggerFactory.getLogger(MockMapOwnerOAuthService.class);
    private static final String DEFAULT_SCOPE = "owner:reviews";

    private final MockMapApiClient mockMapApiClient;
    private final MockMapApiProperties apiProperties;
    private final MockMapOAuthProperties properties;
    private final MockMapOAuthCacheProperties cacheProperties;
    private final MockMapOAuthStateStore oauthStateStore;
    private final MockMapOAuthTokenStore legacyTokenStore;
    private final StoreMockMapConnectionJpaRepository connectionRepository;
    private final StoreProfileJpaRepository storeProfileRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final StoreMembershipService storeMembershipService;
    private final StoreReviewService storeReviewService;

    public MockMapOwnerOAuthService(
            MockMapApiClient mockMapApiClient,
            MockMapApiProperties apiProperties,
            MockMapOAuthProperties properties,
            MockMapOAuthCacheProperties cacheProperties,
            MockMapOAuthStateStore oauthStateStore,
            MockMapOAuthTokenStore legacyTokenStore,
            StoreMockMapConnectionJpaRepository connectionRepository,
            StoreProfileJpaRepository storeProfileRepository,
            UserAccountJpaRepository userAccountRepository,
            StoreMembershipService storeMembershipService,
            @Lazy StoreReviewService storeReviewService
    ) {
        this.mockMapApiClient = mockMapApiClient;
        this.apiProperties = apiProperties;
        this.properties = properties;
        this.cacheProperties = cacheProperties;
        this.oauthStateStore = oauthStateStore;
        this.legacyTokenStore = legacyTokenStore;
        this.connectionRepository = connectionRepository;
        this.storeProfileRepository = storeProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.storeMembershipService = storeMembershipService;
        this.storeReviewService = storeReviewService;
    }

    @Transactional
    public MockMapOAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        storeMembershipService.requireMembership(userId, storeId);
        requireCredentialValues(clientId, clientSecret);

        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        Instant now = Instant.now();
        StoreMockMapConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection == null) {
            connectionRepository.save(StoreMockMapConnectionEntity.of(
                    UUID.randomUUID(),
                    store,
                    user,
                    clientId.trim(),
                    clientSecret.trim(),
                    null,
                    null,
                    null,
                    now
            ));
        } else {
            connection.updateCredentials(clientId.trim(), clientSecret.trim(), now);
        }

        oauthStateStore.deleteAccessToken(storeId);
        return getConnectionStatus(storeId);
    }

    @Transactional
    public MockMapOAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        storeMembershipService.requireMembership(userId, storeId);

        StoreMockMapConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null) {
            revokeIfConnected(connection);
            connectionRepository.delete(connection);
            oauthStateStore.deleteAccessToken(storeId);
        }
        return getConnectionStatus(storeId);
    }

    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        requireRedirectUri();
        requirePlatformOAuthConfigured();
        storeMembershipService.requireMembership(userId, storeId);
        String clientId = resolveOAuthClientId(storeId);

        String state = UUID.randomUUID().toString();
        oauthStateStore.savePendingState(
                state,
                storeId,
                userId,
                pendingStateTtl()
        );

        return UriComponentsBuilder
                .fromUriString(apiProperties.baseUrl())
                .path(resolveAuthorizePath())
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .queryParam("scope", DEFAULT_SCOPE)
                .build(true)
                .toUriString();
    }

    @Transactional
    public synchronized MockMapOAuthConnectionStatus completeAuthorization(String code, String state) {
        requireRedirectUri();
        if (!StringUtils.hasText(code)) {
            throw new MockMapApiException("MockMap OAuth callback에 authorization code가 없습니다.");
        }

        MockMapOAuthPendingState pending = validateState(state);
        storeMembershipService.requireMembership(pending.userId(), pending.storeId());

        String clientId = resolveOAuthClientId(pending.storeId());
        String clientSecret = resolveOAuthClientSecret(pending.storeId());

        MockMapOAuthTokenResponse tokenResponse = mockMapApiClient.exchangeAuthorizationCode(
                clientId,
                clientSecret,
                code,
                properties.redirectUri(),
                state
        );

        StoreMockMapConnectionEntity connection = ensureConnection(
                pending.storeId(),
                pending.userId(),
                clientId,
                clientSecret
        );
        persistStoreConnection(pending.storeId(), pending.userId(), connection, tokenResponse);
        syncReviewsAfterConnect(pending.storeId());
        return getConnectionStatus(pending.storeId());
    }

    public synchronized String getAccessToken(UUID storeId) {
        return oauthStateStore.findAccessToken(storeId)
                .orElseGet(() -> refreshAccessToken(storeId));
    }

    @Transactional(readOnly = true)
    public MockMapOAuthConnectionStatus getConnectionStatus(UUID storeId) {
        return connectionRepository.findByStore_Id(storeId)
                .map(this::toConnectionStatus)
                .orElseGet(() -> isPlatformOAuthConfigured()
                        ? new MockMapOAuthConnectionStatus(
                                true,
                                false,
                                properties.clientId(),
                                null,
                                null,
                                null
                        )
                        : emptyConnectionStatus());
    }

    @Transactional
    public synchronized MockMapOAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        storeMembershipService.requireMembership(userId, storeId);

        StoreMockMapConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null) {
            revokeIfConnected(connection);
            connection.updateCredentials(
                    connection.getClientId(),
                    connection.getClientSecret(),
                    Instant.now()
            );
            oauthStateStore.deleteAccessToken(storeId);
        }

        storeProfileRepository.findById(storeId).ifPresent(store -> {
            StoreProfile current = store.toDomain();
            store.applyDomain(new StoreProfile(
                    current.id(),
                    current.name(),
                    current.phone(),
                    current.introduction(),
                    current.address(),
                    current.category(),
                    current.toneOfVoice(),
                    current.businessHours(),
                    current.menuItems(),
                    null,
                    current.googleReviewUrl(),
                    0,
                    Instant.now()
            ));
        });
        storeReviewService.clearSyncedReviews(storeId);

        return getConnectionStatus(storeId);
    }

    public Optional<String> findConnectedPlaceId(UUID storeId) {
        return connectionRepository.findByStore_Id(storeId)
                .filter(connection -> StringUtils.hasText(connection.getRefreshToken()))
                .filter(connection -> StringUtils.hasText(connection.getPlaceId()))
                .map(StoreMockMapConnectionEntity::getPlaceId);
    }

    public List<UUID> findConnectedStoreIds() {
        return connectionRepository.findAll().stream()
                .filter(connection -> StringUtils.hasText(connection.getRefreshToken()))
                .filter(connection -> StringUtils.hasText(connection.getPlaceId()))
                .map(connection -> connection.getStore().getId())
                .distinct()
                .toList();
    }

    private String refreshAccessToken(UUID storeId) {
        StoreMockMapConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null && StringUtils.hasText(connection.getRefreshToken())) {
            return refreshAndCacheStoreToken(connection);
        }

        if (isPlatformOAuthConfigured()) {
            throw new MockMapApiException(
                    "MockMap OAuth가 연결되지 않았습니다. "
                            + "가게 정보에서 MAP 연동 버튼을 눌러 MockMap 계정으로 로그인해 주세요."
            );
        }

        return refreshLegacyGlobalToken(storeId);
    }

    private void persistStoreConnection(
            UUID storeId,
            UUID userId,
            StoreMockMapConnectionEntity connection,
            MockMapOAuthTokenResponse tokenResponse
    ) {
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.refresh_token())) {
            throw new MockMapApiException("MockMap OAuth 토큰 응답이 비어 있습니다.");
        }
        if (!StringUtils.hasText(tokenResponse.place_id())) {
            throw new MockMapApiException("MockMap OAuth 응답에 place_id가 없습니다.");
        }

        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));

        reconcilePlaceId(store, tokenResponse);

        connection.updateTokens(
                firstNonBlank(tokenResponse.client_id(), connection.getClientId()),
                connection.getClientSecret(),
                tokenResponse.refresh_token(),
                tokenResponse.place_id(),
                firstNonBlank(tokenResponse.place_name(), store.toDomain().name()),
                Instant.now()
        );

        cacheAccessToken(storeId, tokenResponse);
    }

    private void reconcilePlaceId(StoreProfileEntity store, MockMapOAuthTokenResponse tokenResponse) {
        String oauthPlaceId = tokenResponse.place_id();
        StoreProfile current = store.toDomain();

        storeProfileRepository.findByGooglePlaceId(oauthPlaceId)
                .filter(other -> !other.getId().equals(store.getId()))
                .ifPresent(other -> {
                    throw new MockMapApiException(
                            "MockMap place_id(" + oauthPlaceId + ")는 이미 다른 가게("
                                    + other.toDomain().name() + ")에 연결되어 있습니다. "
                                    + "가게마다 서로 다른 MockMap 계정을 연동해 주세요."
                    );
                });

        if (!StringUtils.hasText(current.googlePlaceId())) {
            store.applyDomain(new StoreProfile(
                    current.id(),
                    firstNonBlank(tokenResponse.place_name(), current.name()),
                    current.phone(),
                    current.introduction(),
                    current.address(),
                    current.category(),
                    current.toneOfVoice(),
                    current.businessHours(),
                    current.menuItems(),
                    oauthPlaceId,
                    current.googleReviewUrl(),
                    current.googleTotalReviews(),
                    Instant.now()
            ));
            return;
        }

        if (!current.googlePlaceId().equals(oauthPlaceId)) {
            throw new MockMapApiException(
                    "MockMap OAuth place_id(" + oauthPlaceId + ")와 ShopHub 가게 place_id("
                            + current.googlePlaceId() + ")가 일치하지 않습니다. "
                            + "MockMap 점주 계정과 ShopHub 가게를 같은 place_id로 맞춰 주세요."
            );
        }
    }

    private String refreshAndCacheStoreToken(StoreMockMapConnectionEntity connection) {
        String clientId = resolveOAuthClientId(connection.getStore().getId());
        String clientSecret = resolveOAuthClientSecret(connection.getStore().getId());

        MockMapOAuthTokenResponse tokenResponse = mockMapApiClient.refreshOwnerToken(
                clientId,
                clientSecret,
                connection.getRefreshToken()
        );

        connection.updateTokens(
                firstNonBlank(tokenResponse.client_id(), connection.getClientId()),
                connection.getClientSecret(),
                tokenResponse.refresh_token(),
                firstNonBlank(tokenResponse.place_id(), connection.getPlaceId()),
                firstNonBlank(tokenResponse.place_name(), connection.getPlaceName()),
                Instant.now()
        );

        UUID storeId = connection.getStore().getId();
        cacheAccessToken(storeId, tokenResponse);
        return tokenResponse.access_token();
    }

    private String refreshLegacyGlobalToken(UUID storeId) {
        MockMapOAuthTokenStore.StoredTokens storedTokens = legacyTokenStore.read();
        String refreshToken = storedTokens.refreshToken();
        String clientId = firstNonBlank(storedTokens.clientId(), properties.clientId());
        String clientSecret = properties.clientSecret();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret) || !StringUtils.hasText(refreshToken)) {
            throw new MockMapApiException(
                    "MockMap OAuth가 연결되지 않았습니다. "
                            + "GET /api/integrations/MOCK_MAP/oauth/start?storeId=" + storeId
                            + " 로 MockMap 계정을 연결해 주세요."
            );
        }

        MockMapOAuthTokenResponse tokenResponse = mockMapApiClient.refreshOwnerToken(
                clientId,
                clientSecret,
                refreshToken
        );
        legacyTokenStore.write(new MockMapOAuthTokenStore.StoredTokens(
                tokenResponse.refresh_token(),
                tokenResponse.access_token(),
                firstNonBlank(tokenResponse.client_id(), clientId),
                tokenResponse.place_id(),
                tokenResponse.place_name(),
                Instant.now().toString()
        ));
        return tokenResponse.access_token();
    }

    private void revokeIfConnected(StoreMockMapConnectionEntity connection) {
        if (!StringUtils.hasText(connection.getRefreshToken())) {
            return;
        }
        try {
            mockMapApiClient.revokeToken(
                    resolveOAuthClientId(connection.getStore().getId()),
                    resolveOAuthClientSecret(connection.getStore().getId()),
                    connection.getRefreshToken()
            );
        } catch (MockMapApiException exception) {
            log.warn(
                    "MockMap OAuth revoke 실패: storeId={}, message={}",
                    connection.getStore().getId(),
                    exception.getMessage()
            );
        }
    }

    private MockMapOAuthConnectionStatus toConnectionStatus(StoreMockMapConnectionEntity connection) {
        boolean credentialsConfigured = isPlatformOAuthConfigured()
                || (StringUtils.hasText(connection.getClientId()) && StringUtils.hasText(connection.getClientSecret()));
        String clientId = isPlatformOAuthConfigured()
                ? properties.clientId()
                : firstNonBlank(connection.getClientId(), properties.clientId());
        return new MockMapOAuthConnectionStatus(
                credentialsConfigured,
                StringUtils.hasText(connection.getRefreshToken()),
                clientId,
                connection.getPlaceId(),
                connection.getPlaceName(),
                connection.getUpdatedAt() == null ? null : connection.getUpdatedAt().toString()
        );
    }

    private MockMapOAuthConnectionStatus emptyConnectionStatus() {
        return new MockMapOAuthConnectionStatus(false, false, null, null, null, null);
    }

    private StoreMockMapConnectionEntity requireStoreCredentials(UUID storeId) {
        if (isPlatformOAuthConfigured()) {
            throw new MockMapApiException(
                    "Per-store MockMap credentials are not used when platform OAuth is configured."
            );
        }
        return connectionRepository.findByStore_Id(storeId)
                .filter(connection -> StringUtils.hasText(connection.getClientId())
                        && StringUtils.hasText(connection.getClientSecret()))
                .orElseThrow(() -> new MockMapApiException(
                        "MockMap OAuth credentials가 설정되지 않았습니다. "
                                + "MOCK_MAP_OAUTH_CLIENT_ID/MOCK_MAP_OAUTH_CLIENT_SECRET을 확인하거나 "
                                + "GET /api/integrations/MOCK_MAP/oauth/start?storeId=" + storeId
                                + " 로 연결해 주세요."
                ));
    }

    private boolean isPlatformOAuthConfigured() {
        return StringUtils.hasText(properties.clientId()) && StringUtils.hasText(properties.clientSecret());
    }

    private String resolveOAuthClientId(UUID storeId) {
        if (isPlatformOAuthConfigured()) {
            return properties.clientId();
        }
        return requireStoreCredentials(storeId).getClientId();
    }

    private String resolveOAuthClientSecret(UUID storeId) {
        if (isPlatformOAuthConfigured()) {
            return properties.clientSecret();
        }
        return requireStoreCredentials(storeId).getClientSecret();
    }

    private StoreMockMapConnectionEntity ensureConnection(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        StoreMockMapConnectionEntity existing = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (existing != null) {
            return existing;
        }

        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        return connectionRepository.save(StoreMockMapConnectionEntity.of(
                UUID.randomUUID(),
                store,
                user,
                clientId,
                clientSecret,
                null,
                null,
                null,
                Instant.now()
        ));
    }

    private void syncReviewsAfterConnect(UUID storeId) {
        try {
            storeReviewService.syncMockMapReviewsAndList(storeId);
        } catch (RuntimeException exception) {
            log.warn("MockMap review sync after OAuth failed for storeId={}: {}", storeId, exception.getMessage());
        }
    }

    private void requirePlatformOAuthConfigured() {
        if (!isPlatformOAuthConfigured()) {
            throw new MockMapApiException(
                    "MockMap platform OAuth가 설정되지 않았습니다. "
                            + "MOCK_MAP_OAUTH_CLIENT_ID와 MOCK_MAP_OAUTH_CLIENT_SECRET을 BE .env에 설정해 주세요."
            );
        }
    }

    private void requireCredentialValues(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalArgumentException("clientId와 clientSecret은 필수입니다.");
        }
    }

    private void requireRedirectUri() {
        if (!StringUtils.hasText(properties.redirectUri())) {
            throw new MockMapApiException(
                    "MockMap OAuth redirect_uri가 설정되지 않았습니다. MOCK_MAP_OAUTH_REDIRECT_URI를 확인해 주세요."
            );
        }
    }

    private void cacheAccessToken(UUID storeId, MockMapOAuthTokenResponse tokenResponse) {
        int expiresIn = tokenResponse.expires_in() == null ? 900 : tokenResponse.expires_in();
        int safeTtlSeconds = Math.max(1, expiresIn - 30);
        oauthStateStore.saveAccessToken(
                storeId,
                tokenResponse.access_token(),
                Duration.ofSeconds(safeTtlSeconds)
        );
    }

    private MockMapOAuthPendingState validateState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new MockMapApiException("MockMap OAuth callback state가 없습니다.");
        }
        return oauthStateStore.consumePendingState(state)
                .orElseThrow(() -> new MockMapApiException("MockMap OAuth state가 유효하지 않거나 만료되었습니다."));
    }

    private Duration pendingStateTtl() {
        return Duration.ofSeconds(Math.max(60, cacheProperties.getPendingStateTtlSeconds()));
    }

    private String resolveAuthorizePath() {
        String path = StringUtils.hasText(properties.authorizePath())
                ? properties.authorizePath()
                : "/oauth/authorize/";
        return path.startsWith("/") ? path : "/" + path;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
