package org.hackathon12.shophub.infrastructure.instagram.oauth;

import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.infrastructure.instagram.InstagramGraphProperties;
import org.hackathon12.shophub.infrastructure.persistence.StoreInstagramConnectionEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreInstagramConnectionJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreProfileJpaRepository;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountEntity;
import org.hackathon12.shophub.infrastructure.persistence.UserAccountJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class InstagramOwnerOAuthService {

    private static final String PSEUDO_OAUTH_CODE = "connected";

    private final InstagramGraphProperties graphProperties;
    private final InstagramOAuthProperties oauthProperties;
    private final InstagramOAuthStateStore oauthStateStore;
    private final StoreInstagramConnectionJpaRepository connectionRepository;
    private final StoreProfileJpaRepository storeProfileRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final StoreMembershipService storeMembershipService;

    public InstagramOwnerOAuthService(
            InstagramGraphProperties graphProperties,
            InstagramOAuthProperties oauthProperties,
            InstagramOAuthStateStore oauthStateStore,
            StoreInstagramConnectionJpaRepository connectionRepository,
            StoreProfileJpaRepository storeProfileRepository,
            UserAccountJpaRepository userAccountRepository,
            StoreMembershipService storeMembershipService
    ) {
        this.graphProperties = graphProperties;
        this.oauthProperties = oauthProperties;
        this.oauthStateStore = oauthStateStore;
        this.connectionRepository = connectionRepository;
        this.storeProfileRepository = storeProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.storeMembershipService = storeMembershipService;
    }

    @Transactional
    public InstagramOAuthConnectionStatus saveCredentials(
            UUID storeId,
            UUID userId,
            String clientId,
            String clientSecret
    ) {
        storeMembershipService.requireMembership(userId, storeId);
        requireAppCredentialsConfigured();
        return getConnectionStatus(storeId);
    }

    @Transactional
    public InstagramOAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
        return disconnect(storeId, userId);
    }

    @Transactional
    public String buildAuthorizationUrl(UUID storeId, UUID userId) {
        requireRedirectUri();
        requireAppCredentialsConfigured();
        storeMembershipService.requireMembership(userId, storeId);

        String state = UUID.randomUUID().toString();
        oauthStateStore.savePendingState(state, storeId, userId, pendingStateTtl());

        return UriComponentsBuilder
                .fromUriString(oauthProperties.redirectUri())
                .queryParam("state", state)
                .queryParam("code", PSEUDO_OAUTH_CODE)
                .build(true)
                .toUriString();
    }

    @Transactional
    public synchronized InstagramOAuthConnectionStatus completeAuthorization(String code, String state) {
        requireRedirectUri();
        requireAppCredentialsConfigured();
        if (!StringUtils.hasText(code)) {
            throw new InstagramOAuthException("Instagram OAuth callback에 authorization code가 없습니다.");
        }
        if (!StringUtils.hasText(state)) {
            throw new InstagramOAuthException("Instagram OAuth state가 유효하지 않거나 만료되었습니다.");
        }

        InstagramOAuthPendingState pending = oauthStateStore.consumePendingState(state)
                .orElseThrow(() -> new InstagramOAuthException("Instagram OAuth state가 유효하지 않거나 만료되었습니다."));
        storeMembershipService.requireMembership(pending.userId(), pending.storeId());
        persistConnection(pending.storeId(), pending.userId());
        return getConnectionStatus(pending.storeId());
    }

    @Transactional(readOnly = true)
    public InstagramOAuthConnectionStatus getConnectionStatus(UUID storeId) {
        boolean credentialsConfigured = hasAppCredentialsConfigured();
        return connectionRepository.findByStore_Id(storeId)
                .map(connection -> toConnectionStatus(connection, credentialsConfigured))
                .orElse(emptyConnectionStatus(credentialsConfigured));
    }

    @Transactional
    public synchronized InstagramOAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        storeMembershipService.requireMembership(userId, storeId);

        StoreInstagramConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null) {
            connection.clearConnection(Instant.now());
        }
        return getConnectionStatus(storeId);
    }

    public void requireStoreConnected(UUID storeId) {
        InstagramOAuthConnectionStatus status = getConnectionStatus(storeId);
        if (!status.connected()) {
            throw new InstagramOAuthException(
                    "Instagram 계정이 연결되지 않았습니다. "
                            + "GET /api/integrations/INSTAGRAM/oauth/start?storeId=" + storeId
                            + " 로 연결해 주세요."
            );
        }
    }

    private void persistConnection(UUID storeId, UUID userId) {
        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        Instant now = Instant.now();
        String accountId = graphProperties.accountId().trim();
        String username = safeUsername();

        StoreInstagramConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection == null) {
            connectionRepository.save(StoreInstagramConnectionEntity.of(
                    UUID.randomUUID(),
                    store,
                    user,
                    accountId,
                    username,
                    now
            ));
            return;
        }

        connection.updateConnection(accountId, username, now);
    }

    private InstagramOAuthConnectionStatus toConnectionStatus(
            StoreInstagramConnectionEntity connection,
            boolean credentialsConfigured
    ) {
        boolean connected = credentialsConfigured && StringUtils.hasText(connection.getInstagramAccountId());
        return new InstagramOAuthConnectionStatus(
                credentialsConfigured,
                connected,
                graphProperties.accountId(),
                connected ? connection.getInstagramAccountId() : null,
                connected ? formatUsername(connection.getInstagramUsername()) : null,
                connection.getUpdatedAt().toString()
        );
    }

    private InstagramOAuthConnectionStatus emptyConnectionStatus(boolean credentialsConfigured) {
        return new InstagramOAuthConnectionStatus(
                credentialsConfigured,
                false,
                credentialsConfigured ? graphProperties.accountId() : null,
                null,
                null,
                null
        );
    }

    private boolean hasAppCredentialsConfigured() {
        return StringUtils.hasText(graphProperties.accountId())
                && StringUtils.hasText(graphProperties.accessToken());
    }

    private void requireAppCredentialsConfigured() {
        if (!hasAppCredentialsConfigured()) {
            throw new InstagramOAuthException("Instagram Graph API 계정/토큰이 설정되지 않았습니다.");
        }
        if (StringUtils.hasText(graphProperties.allowedAccountId())
                && !graphProperties.allowedAccountId().equals(graphProperties.accountId())) {
            throw new InstagramOAuthException(
                    "Instagram 게시는 @" + safeUsername() + " 계정만 사용할 수 있습니다."
            );
        }
    }

    private void requireRedirectUri() {
        if (!StringUtils.hasText(oauthProperties.redirectUri())) {
            throw new InstagramOAuthException("Instagram OAuth redirect URI가 설정되지 않았습니다.");
        }
    }

    private Duration pendingStateTtl() {
        long ttlSeconds = oauthProperties.pendingStateTtlSeconds() == null
                ? 600L
                : oauthProperties.pendingStateTtlSeconds();
        return Duration.ofSeconds(ttlSeconds);
    }

    private String safeUsername() {
        return StringUtils.hasText(graphProperties.allowedUsername())
                ? graphProperties.allowedUsername()
                : "commentcopybot";
    }

    private String formatUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return username.startsWith("@") ? username : "@" + username;
    }
}
