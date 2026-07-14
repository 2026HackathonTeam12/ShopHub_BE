package org.hackathon12.shophub.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_store_memberships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_store_memberships_user_store", columnNames = {"user_id", "store_id"})
        }
)
public class UserStoreMembershipEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_store_memberships_user")
    )
    private UserAccountEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_store_memberships_store")
    )
    private StoreProfileEntity store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreMembershipRole role;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserStoreMembershipEntity() {
    }

    public static UserStoreMembershipEntity of(
            UUID id,
            UserAccountEntity user,
            StoreProfileEntity store,
            StoreMembershipRole role,
            Instant createdAt
    ) {
        UserStoreMembershipEntity entity = new UserStoreMembershipEntity();
        entity.id = id;
        entity.user = user;
        entity.store = store;
        entity.role = role;
        entity.createdAt = createdAt;
        return entity;
    }

    public StoreProfileEntity getStore() {
        return store;
    }
}
