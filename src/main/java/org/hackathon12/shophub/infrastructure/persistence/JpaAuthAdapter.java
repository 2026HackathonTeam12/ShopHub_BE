package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.auth.model.UserAccount;
import org.hackathon12.shophub.domain.auth.port.AuthPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAuthAdapter implements AuthPort {

    private final UserAccountJpaRepository userAccountJpaRepository;

    public JpaAuthAdapter(UserAccountJpaRepository userAccountJpaRepository) {
        this.userAccountJpaRepository = userAccountJpaRepository;
    }

    @Override
    public UserAccount findByEmail(String email) {
        return userAccountJpaRepository.findByEmail(email)
                .map(UserAccountEntity::toDomain)
                .orElse(null);
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        UserAccountEntity saved = userAccountJpaRepository.save(UserAccountEntity.fromDomain(userAccount));
        return saved.toDomain();
    }
}
