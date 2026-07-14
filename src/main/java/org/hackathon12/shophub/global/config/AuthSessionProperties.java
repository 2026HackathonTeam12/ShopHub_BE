package org.hackathon12.shophub.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.session")
public class AuthSessionProperties {

    private String store = "redis";
    private int ttlHours = 24;
    private String keyPrefix = "shophub:auth:session:";

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public int getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(int ttlHours) {
        this.ttlHours = ttlHours;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration ttl() {
        return Duration.ofHours(Math.max(1, ttlHours));
    }
}
