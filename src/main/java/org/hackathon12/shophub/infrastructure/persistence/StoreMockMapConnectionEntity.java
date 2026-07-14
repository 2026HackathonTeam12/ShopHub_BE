package org.hackathon12.shophub.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_mockmap_connections")
public class StoreMockMapConnectionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_store_mockmap_connections_store")
    )
    private StoreProfileEntity store;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "connected_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_mockmap_connections_user")
    )
    private UserAccountEntity connectedByUser;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    @Column
    private String refreshToken;

    @Column
    private String placeId;

    @Column
    private String placeName;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StoreMockMapConnectionEntity() {
    }

    private static String nullableText(String value) {
        return value == null ? "" : value;
    }

    public static StoreMockMapConnectionEntity of(
            UUID id,
            StoreProfileEntity store,
            UserAccountEntity connectedByUser,
            String clientId,
            String clientSecret,
            String refreshToken,
            String placeId,
            String placeName,
            Instant updatedAt
    ) {
        StoreMockMapConnectionEntity entity = new StoreMockMapConnectionEntity();
        entity.id = id;
        entity.store = store;
        entity.connectedByUser = connectedByUser;
        entity.clientId = clientId;
        entity.clientSecret = clientSecret;
        entity.refreshToken = nullableText(refreshToken);
        entity.placeId = nullableText(placeId);
        entity.placeName = nullableText(placeName);
        entity.updatedAt = updatedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public StoreProfileEntity getStore() {
        return store;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateCredentials(String clientId, String clientSecret, Instant updatedAt) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = "";
        this.placeId = "";
        this.placeName = "";
        this.updatedAt = updatedAt;
    }

    public void updateTokens(
            String clientId,
            String clientSecret,
            String refreshToken,
            String placeId,
            String placeName,
            Instant updatedAt
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = nullableText(refreshToken);
        this.placeId = nullableText(placeId);
        this.placeName = nullableText(placeName);
        this.updatedAt = updatedAt;
    }
}
