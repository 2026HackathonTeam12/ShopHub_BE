package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserStoreMembershipJpaRepository extends JpaRepository<UserStoreMembershipEntity, UUID> {
}
