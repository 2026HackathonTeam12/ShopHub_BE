package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.auth.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 191)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    protected UserAccountEntity() {
    }

    public static UserAccountEntity fromDomain(UserAccount userAccount) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.id = userAccount.id();
        entity.email = userAccount.email();
        entity.password = userAccount.password();
        entity.name = userAccount.name();
        return entity;
    }

    public UserAccount toDomain() {
        return new UserAccount(id, email, password, name);
    }

    public UUID getId() {
        return id;
    }
}
