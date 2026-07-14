package org.hackathon12.shophub.infrastructure.x.oauth;

import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.StoreXConnectionEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreXConnectionJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountEntity;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountJpaRepository;
import org.hackathon12.shophub.infrastructure.x.XApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class XOwnerOAuthService {

    private static final Logger log = LoggerFactory.getLogger(XOwnerOAuthService.class);

    private final XOAuthApiClient xOAuthApiClient;
    private final XOAuthProperties properties;
    private final XOAuthCacheProperties cacheProperties;
    private final XOAuthStateStore oauthStateStore;
    private final StoreXConnectionJpaRepository connectionRepository;
    private final StoreProfileJpaRepository storeProfileRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final StoreMembershipService storeMembershipService;

    public XOwnerOAuthService(
            XOAuthApiClient xOAuthApiClient,
            XOAuthProperties properties,
            XOAuthCacheProperties cacheProperties,
            XOAuthStateStore oauthStateStore,
            StoreXConnectionJpaRepository connectionRepository,
            StoreProfileJpaRepository storeProfileRepository,
            UserAccountJpaRepository userAccountRepository,
            StoreMembershipService storeMembershipService
    ) {
        this.xOAuthApiClient = xOAuthApiClient;
        this.properties = properties;
        this.cacheProperties = cacheProperties;
        this.oauthStateStore = oauthStateStore;
        this.connectionRepository = connectionRepository;
        this.storeProfileRepository = storeProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.storeMembershipService = storeMembershipService;
    }

    @Transactional
    public XOAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        storeMembershipService.requireMembership(userId, storeId);
        requireAppCredentialsConfigured();
        ensureConnectionShell(storeId, userId);
        return getConnectionStatus(storeId);
    }

    @Transactional
    public XOAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        storeMembershipService.requireMembership(userId, storeId);

        StoreXConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null) {
            revokeIfConnected(connection);
            connectionRepository.delete(connection);
            oauthStateStore.deleteAccessToken(storeId);
        }
        return getConnectionStatus(storeId);
    }

    @Transactional
    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        requireRedirectUri();
        requireAppCredentialsConfigured();
        storeMembershipService.requireMembership(userId, storeId);
        ensureConnectionShell(storeId, userId);

        String state = UUID.randomUUID().toString();
        String codeVerifier = XPkceSupport.generateCodeVerifier();
        String codeChallenge = XPkceSupport.generateCodeChallenge(codeVerifier);

        oauthStateStore.savePendingState(
                state,
                storeId,
                userId,
                codeVerifier,
                pendingStateTtl()
        );

        return UriComponentsBuilder
                .fromUriString(properties.authorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", properties.scope())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    @Transactional
    public synchronized XOAuthConnectionStatus completeAuthorization(String code, String state) {
        requireRedirectUri();
        if (!StringUtils.hasText(code)) {
            throw new XApiException("X OAuth callback에 authorization code가 없습니다.");
        }

        XOAuthPendingState pending = validateState(state);
        storeMembershipService.requireMembership(pending.userId(), pending.storeId());

        XOAuthTokenResponse tokenResponse = xOAuthApiClient.exchangeAuthorizationCode(
                code,
                pending.codeVerifier()
        );
        if (!StringUtils.hasText(tokenResponse.refresh_token())) {
            throw new XApiException("X OAuth refresh_token이 없습니다. offline.access scope를 확인해 주세요.");
        }

        Map<String, Object> userResponse = xOAuthApiClient.getCurrentUser(tokenResponse.access_token());
        Map<String, Object> data = castMap(userResponse.get("data"));
        String xUserId = valueAsString(data.get("id"), "X user id가 없습니다.");
        String xUsername = valueAsString(data.get("username"), "X username이 없습니다.");

        persistConnection(
                pending.storeId(),
                pending.userId(),
                tokenResponse,
                xUserId,
                xUsername
        );
        return getConnectionStatus(pending.storeId());
    }

    public synchronized String getAccessToken(UUID storeId) {
        return oauthStateStore.findAccessToken(storeId)
                .orElseGet(() -> refreshAccessToken(storeId));
    }

    public synchronized void invalidateAccessToken(UUID storeId) {
        oauthStateStore.deleteAccessToken(storeId);
    }

    @Transactional(readOnly = true)
    public XOAuthConnectionStatus getConnectionStatus(UUID storeId) {
        boolean credentialsConfigured = isAppCredentialsConfigured();
        return connectionRepository.findByStore_Id(storeId)
                .map(connection -> toConnectionStatus(connection, credentialsConfigured))
                .orElse(new XOAuthConnectionStatus(
                        credentialsConfigured,
                        false,
                        maskedClientId(),
                        null,
                        null,
                        null
                ));
    }

    @Transactional
    public synchronized XOAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        storeMembershipService.requireMembership(userId, storeId);

        StoreXConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null) {
            revokeIfConnected(connection);
            connection.clearTokens(Instant.now());
            oauthStateStore.deleteAccessToken(storeId);
        }
        return getConnectionStatus(storeId);
    }

    public XOAuthConnectionStatus getConnectedAccount(UUID storeId) {
        XOAuthConnectionStatus status = getConnectionStatus(storeId);
        if (!status.connected()) {
            throw new IllegalArgumentException(
                    "X OAuth가 연결되지 않았습니다. "
                            + "GET /api/integrations/X/oauth/start?storeId=" + storeId
                            + " 로 연결해 주세요."
            );
        }
        return status;
    }

    private String refreshAccessToken(UUID storeId) {
        StoreXConnectionEntity connection = connectionRepository.findByStore_Id(storeId)
                .orElseThrow(() -> new XApiException(
                        "X OAuth가 연결되지 않았습니다. "
                                + "GET /api/integrations/X/oauth/start?storeId=" + storeId
                                + " 로 연결해 주세요."
                ));

        if (!StringUtils.hasText(connection.getRefreshToken())) {
            throw new XApiException("X OAuth refresh_token이 없습니다. 다시 연결해 주세요.");
        }

        XOAuthTokenResponse tokenResponse = xOAuthApiClient.refreshToken(connection.getRefreshToken());
        connection.updateTokens(
                firstNonBlank(tokenResponse.refresh_token(), connection.getRefreshToken()),
                connection.getXUserId(),
                connection.getXUsername(),
                Instant.now()
        );
        cacheAccessToken(storeId, tokenResponse);
        return tokenResponse.access_token();
    }

    private void persistConnection(
            UUID storeId,
            UUID userId,
            XOAuthTokenResponse tokenResponse,
            String xUserId,
            String xUsername
    ) {
        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        Instant now = Instant.now();
        StoreXConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection == null) {
            connectionRepository.save(StoreXConnectionEntity.of(
                    UUID.randomUUID(),
                    store,
                    user,
                    tokenResponse.refresh_token(),
                    xUserId,
                    xUsername,
                    now
            ));
        } else {
            connection.updateTokens(
                    tokenResponse.refresh_token(),
                    xUserId,
                    xUsername,
                    now
            );
        }

        cacheAccessToken(storeId, tokenResponse);
    }

    private void ensureConnectionShell(UUID storeId, UUID userId) {
        if (connectionRepository.findByStore_Id(storeId).isPresent()) {
            return;
        }

        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        connectionRepository.save(StoreXConnectionEntity.of(
                UUID.randomUUID(),
                store,
                user,
                null,
                null,
                null,
                Instant.now()
        ));
    }

    private void revokeIfConnected(StoreXConnectionEntity connection) {
        if (!StringUtils.hasText(connection.getRefreshToken())) {
            return;
        }
        try {
            xOAuthApiClient.revokeRefreshToken(connection.getRefreshToken());
        } catch (XApiException exception) {
            log.warn(
                    "X OAuth revoke 실패: storeId={}, message={}",
                    connection.getStore().getId(),
                    exception.getMessage()
            );
        }
    }

    private XOAuthConnectionStatus toConnectionStatus(
            StoreXConnectionEntity connection,
            boolean credentialsConfigured
    ) {
        return new XOAuthConnectionStatus(
                credentialsConfigured,
                StringUtils.hasText(connection.getRefreshToken()),
                maskedClientId(),
                blankToNull(connection.getXUserId()),
                blankToNull(connection.getXUsername()),
                connection.getUpdatedAt() == null ? null : connection.getUpdatedAt().toString()
        );
    }

    private XOAuthPendingState validateState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new XApiException("X OAuth callback state가 없습니다.");
        }
        return oauthStateStore.consumePendingState(state)
                .orElseThrow(() -> new XApiException("X OAuth state가 유효하지 않거나 만료되었습니다."));
    }

    private void cacheAccessToken(UUID storeId, XOAuthTokenResponse tokenResponse) {
        int expiresIn = tokenResponse.expires_in() == null ? 7200 : tokenResponse.expires_in();
        int safeTtlSeconds = Math.max(1, expiresIn - 30);
        oauthStateStore.saveAccessToken(
                storeId,
                tokenResponse.access_token(),
                Duration.ofSeconds(safeTtlSeconds)
        );
    }

    private void requireAppCredentialsConfigured() {
        if (!isAppCredentialsConfigured()) {
            throw new XApiException(
                    "X OAuth app credentials가 설정되지 않았습니다. "
                            + "X_OAUTH_CLIENT_ID / X_OAUTH_CLIENT_SECRET을 확인해 주세요."
            );
        }
    }

    private boolean isAppCredentialsConfigured() {
        return StringUtils.hasText(properties.clientId()) && StringUtils.hasText(properties.clientSecret());
    }

    private void requireRedirectUri() {
        if (!StringUtils.hasText(properties.redirectUri())) {
            throw new XApiException(
                    "X OAuth redirect_uri가 설정되지 않았습니다. X_OAUTH_REDIRECT_URI를 확인해 주세요."
            );
        }
    }

    private Duration pendingStateTtl() {
        return Duration.ofSeconds(Math.max(60, cacheProperties.getPendingStateTtlSeconds()));
    }

    private String maskedClientId() {
        if (!StringUtils.hasText(properties.clientId())) {
            return null;
        }
        String clientId = properties.clientId();
        if (clientId.length() <= 8) {
            return clientId;
        }
        return clientId.substring(0, 4) + "..." + clientId.substring(clientId.length() - 4);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new XApiException("X OAuth 사용자 응답 형식이 올바르지 않습니다.");
    }

    private String valueAsString(Object value, String messageIfNull) {
        if (value == null) {
            throw new XApiException(messageIfNull);
        }
        return String.valueOf(value);
    }
}
