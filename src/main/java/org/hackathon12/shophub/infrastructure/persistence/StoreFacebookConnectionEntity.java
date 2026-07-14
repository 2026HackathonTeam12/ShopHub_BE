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
@Table(name = "store_facebook_connections")
public class StoreFacebookConnectionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_store_facebook_connections_store")
    )
    private StoreProfileEntity store;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "connected_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_facebook_connections_user")
    )
    private UserAccountEntity connectedByUser;

    @Column
    private String facebookPageId;

    @Column
    private String facebookPageName;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StoreFacebookConnectionEntity() {
    }

    private static String nullableText(String value) {
        return value == null ? "" : value;
    }

    public static StoreFacebookConnectionEntity of(
            UUID id,
            StoreProfileEntity store,
            UserAccountEntity connectedByUser,
            String facebookPageId,
            String facebookPageName,
            Instant updatedAt
    ) {
        StoreFacebookConnectionEntity entity = new StoreFacebookConnectionEntity();
        entity.id = id;
        entity.store = store;
        entity.connectedByUser = connectedByUser;
        entity.facebookPageId = nullableText(facebookPageId);
        entity.facebookPageName = nullableText(facebookPageName);
        entity.updatedAt = updatedAt;
        return entity;
    }

    public String getFacebookPageId() {
        return facebookPageId;
    }

    public String getFacebookPageName() {
        return facebookPageName;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateConnection(String facebookPageId, String facebookPageName, Instant updatedAt) {
        this.facebookPageId = nullableText(facebookPageId);
        this.facebookPageName = nullableText(facebookPageName);
        this.updatedAt = updatedAt;
    }

    public void clearConnection(Instant updatedAt) {
        this.facebookPageId = "";
        this.facebookPageName = "";
        this.updatedAt = updatedAt;
    }
}
