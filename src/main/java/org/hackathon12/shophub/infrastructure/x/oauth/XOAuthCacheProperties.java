package org.hackathon12.shophub.infrastructure.x.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "x.oauth.cache")
public class XOAuthCacheProperties {

    private String store = "redis";
    private long pendingStateTtlSeconds = 600;
    private String pendingStateKeyPrefix = "shophub:x:oauth:pending-state:";
    private String accessTokenKeyPrefix = "shophub:x:oauth:access-token:";

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public long getPendingStateTtlSeconds() {
        return pendingStateTtlSeconds;
    }

    public void setPendingStateTtlSeconds(long pendingStateTtlSeconds) {
        this.pendingStateTtlSeconds = pendingStateTtlSeconds;
    }

    public String getPendingStateKeyPrefix() {
        return pendingStateKeyPrefix;
    }

    public void setPendingStateKeyPrefix(String pendingStateKeyPrefix) {
        this.pendingStateKeyPrefix = pendingStateKeyPrefix;
    }

    public String getAccessTokenKeyPrefix() {
        return accessTokenKeyPrefix;
    }

    public void setAccessTokenKeyPrefix(String accessTokenKeyPrefix) {
        this.accessTokenKeyPrefix = accessTokenKeyPrefix;
    }
}
