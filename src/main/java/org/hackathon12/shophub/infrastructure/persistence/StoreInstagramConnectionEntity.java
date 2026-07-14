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
@Table(name = "store_instagram_connections")
public class StoreInstagramConnectionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_store_instagram_connections_store")
    )
    private StoreProfileEntity store;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "connected_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_instagram_connections_user")
    )
    private UserAccountEntity connectedByUser;

    @Column
    private String instagramAccountId;

    @Column
    private String instagramUsername;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StoreInstagramConnectionEntity() {
    }

    private static String nullableText(String value) {
        return value == null ? "" : value;
    }

    public static StoreInstagramConnectionEntity of(
            UUID id,
            StoreProfileEntity store,
            UserAccountEntity connectedByUser,
            String instagramAccountId,
            String instagramUsername,
            Instant updatedAt
    ) {
        StoreInstagramConnectionEntity entity = new StoreInstagramConnectionEntity();
        entity.id = id;
        entity.store = store;
        entity.connectedByUser = connectedByUser;
        entity.instagramAccountId = nullableText(instagramAccountId);
        entity.instagramUsername = nullableText(instagramUsername);
        entity.updatedAt = updatedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public StoreProfileEntity getStore() {
        return store;
    }

    public String getInstagramAccountId() {
        return instagramAccountId;
    }

    public String getInstagramUsername() {
        return instagramUsername;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateConnection(String instagramAccountId, String instagramUsername, Instant updatedAt) {
        this.instagramAccountId = nullableText(instagramAccountId);
        this.instagramUsername = nullableText(instagramUsername);
        this.updatedAt = updatedAt;
    }

    public void clearConnection(Instant updatedAt) {
        this.instagramAccountId = "";
        this.instagramUsername = "";
        this.updatedAt = updatedAt;
    }
}
