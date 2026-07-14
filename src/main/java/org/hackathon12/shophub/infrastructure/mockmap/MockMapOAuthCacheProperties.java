package org.hackathon12.shophub.infrastructure.mockmap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock-map.oauth.cache")
public class MockMapOAuthCacheProperties {

    private String store = "redis";
    private int pendingStateTtlSeconds = 600;
    private String pendingStateKeyPrefix = "shophub:mockmap:oauth:pending-state:";
    private String accessTokenKeyPrefix = "shophub:mockmap:oauth:access-token:";

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public int getPendingStateTtlSeconds() {
        return pendingStateTtlSeconds;
    }

    public void setPendingStateTtlSeconds(int pendingStateTtlSeconds) {
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
