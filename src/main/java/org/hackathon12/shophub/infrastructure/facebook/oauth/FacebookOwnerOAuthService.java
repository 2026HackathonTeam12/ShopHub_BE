package org.hackathon12.shophub.infrastructure.facebook.oauth;

import org.hackathon12.shophub.domain.store.service.StoreMembershipService;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.infrastructure.facebook.FacebookGraphProperties;
import org.hackathon12.shophub.infrastructure.persistence.StoreFacebookConnectionEntity;
import org.hackathon12.shophub.infrastructure.persistence.StoreFacebookConnectionJpaRepository;
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
public class FacebookOwnerOAuthService {

    private static final String PSEUDO_OAUTH_CODE = "connected";

    private final FacebookGraphProperties graphProperties;
    private final FacebookOAuthProperties oauthProperties;
    private final FacebookOAuthStateStore oauthStateStore;
    private final StoreFacebookConnectionJpaRepository connectionRepository;
    private final StoreProfileJpaRepository storeProfileRepository;
    private final UserAccountJpaRepository userAccountRepository;
    private final StoreMembershipService storeMembershipService;

    public FacebookOwnerOAuthService(
            FacebookGraphProperties graphProperties,
            FacebookOAuthProperties oauthProperties,
            FacebookOAuthStateStore oauthStateStore,
            StoreFacebookConnectionJpaRepository connectionRepository,
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
    public FacebookOAuthConnectionStatus saveCredentials(
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
    public FacebookOAuthConnectionStatus deleteCredentials(UUID storeId, UUID userId) {
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
    public synchronized FacebookOAuthConnectionStatus completeAuthorization(String code, String state) {
        requireRedirectUri();
        requireAppCredentialsConfigured();
        if (!StringUtils.hasText(code)) {
            throw new FacebookOAuthException("Facebook OAuth callback에 authorization code가 없습니다.");
        }
        if (!StringUtils.hasText(state)) {
            throw new FacebookOAuthException("Facebook OAuth state가 유효하지 않거나 만료되었습니다.");
        }

        FacebookOAuthPendingState pending = oauthStateStore.consumePendingState(state)
                .orElseThrow(() -> new FacebookOAuthException("Facebook OAuth state가 유효하지 않거나 만료되었습니다."));
        storeMembershipService.requireMembership(pending.userId(), pending.storeId());
        persistConnection(pending.storeId(), pending.userId());
        return getConnectionStatus(pending.storeId());
    }

    @Transactional(readOnly = true)
    public FacebookOAuthConnectionStatus getConnectionStatus(UUID storeId) {
        boolean credentialsConfigured = hasAppCredentialsConfigured();
        return connectionRepository.findByStore_Id(storeId)
                .map(connection -> toConnectionStatus(connection, credentialsConfigured))
                .orElse(emptyConnectionStatus(credentialsConfigured));
    }

    @Transactional
    public synchronized FacebookOAuthConnectionStatus disconnect(UUID storeId, UUID userId) {
        storeMembershipService.requireMembership(userId, storeId);

        StoreFacebookConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection != null) {
            connection.clearConnection(Instant.now());
        }
        return getConnectionStatus(storeId);
    }

    private void persistConnection(UUID storeId, UUID userId) {
        StoreProfileEntity store = storeProfileRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없습니다. storeId=" + storeId));
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId));

        Instant now = Instant.now();
        String pageId = graphProperties.pageId().trim();
        String pageName = safePageName();

        StoreFacebookConnectionEntity connection = connectionRepository.findByStore_Id(storeId).orElse(null);
        if (connection == null) {
            connectionRepository.save(StoreFacebookConnectionEntity.of(
                    UUID.randomUUID(),
                    store,
                    user,
                    pageId,
                    pageName,
                    now
            ));
            return;
        }

        connection.updateConnection(pageId, pageName, now);
    }

    private FacebookOAuthConnectionStatus toConnectionStatus(
            StoreFacebookConnectionEntity connection,
            boolean credentialsConfigured
    ) {
        boolean connected = credentialsConfigured && StringUtils.hasText(connection.getFacebookPageId());
        return new FacebookOAuthConnectionStatus(
                credentialsConfigured,
                connected,
                graphProperties.pageId(),
                connected ? connection.getFacebookPageId() : null,
                connected ? connection.getFacebookPageName() : null,
                connection.getUpdatedAt().toString()
        );
    }

    private FacebookOAuthConnectionStatus emptyConnectionStatus(boolean credentialsConfigured) {
        return new FacebookOAuthConnectionStatus(
                credentialsConfigured,
                false,
                credentialsConfigured ? graphProperties.pageId() : null,
                null,
                null,
                null
        );
    }

    private boolean hasAppCredentialsConfigured() {
        return StringUtils.hasText(graphProperties.pageId())
                && StringUtils.hasText(graphProperties.accessToken());
    }

    private void requireAppCredentialsConfigured() {
        if (!hasAppCredentialsConfigured()) {
            throw new FacebookOAuthException("Facebook Graph API 페이지/토큰이 설정되지 않았습니다.");
        }
        if (StringUtils.hasText(graphProperties.allowedPageId())
                && !graphProperties.allowedPageId().equals(graphProperties.pageId())) {
            throw new FacebookOAuthException(
                    "Facebook 게시는 " + safePageName() + " 페이지만 사용할 수 있습니다."
            );
        }
    }

    private void requireRedirectUri() {
        if (!StringUtils.hasText(oauthProperties.redirectUri())) {
            throw new FacebookOAuthException("Facebook OAuth redirect URI가 설정되지 않았습니다.");
        }
    }

    private Duration pendingStateTtl() {
        long ttlSeconds = oauthProperties.pendingStateTtlSeconds() == null
                ? 600L
                : oauthProperties.pendingStateTtlSeconds();
        return Duration.ofSeconds(ttlSeconds);
    }

    private String safePageName() {
        return StringUtils.hasText(graphProperties.allowedPageName())
                ? graphProperties.allowedPageName()
                : "commentcopybot";
    }
}
