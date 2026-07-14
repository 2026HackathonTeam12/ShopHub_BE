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
@Table(name = "store_x_connections")
public class StoreXConnectionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_store_x_connections_store")
    )
    private StoreProfileEntity store;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "connected_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_x_connections_user")
    )
    private UserAccountEntity connectedByUser;

    @Column
    private String refreshToken;

    @Column
    private String xUserId;

    @Column
    private String xUsername;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StoreXConnectionEntity() {
    }

    private static String nullableText(String value) {
        return value == null ? "" : value;
    }

    public static StoreXConnectionEntity of(
            UUID id,
            StoreProfileEntity store,
            UserAccountEntity connectedByUser,
            String refreshToken,
            String xUserId,
            String xUsername,
            Instant updatedAt
    ) {
        StoreXConnectionEntity entity = new StoreXConnectionEntity();
        entity.id = id;
        entity.store = store;
        entity.connectedByUser = connectedByUser;
        entity.refreshToken = nullableText(refreshToken);
        entity.xUserId = nullableText(xUserId);
        entity.xUsername = nullableText(xUsername);
        entity.updatedAt = updatedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public StoreProfileEntity getStore() {
        return store;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getXUserId() {
        return xUserId;
    }

    public String getXUsername() {
        return xUsername;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void clearTokens(Instant updatedAt) {
        this.refreshToken = "";
        this.xUserId = "";
        this.xUsername = "";
        this.updatedAt = updatedAt;
    }

    public void updateTokens(
            String refreshToken,
            String xUserId,
            String xUsername,
            Instant updatedAt
    ) {
        this.refreshToken = nullableText(refreshToken);
        this.xUserId = nullableText(xUserId);
        this.xUsername = nullableText(xUsername);
        this.updatedAt = updatedAt;
    }
}
