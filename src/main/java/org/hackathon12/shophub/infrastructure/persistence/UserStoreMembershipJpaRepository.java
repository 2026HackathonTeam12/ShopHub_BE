package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserStoreMembershipJpaRepository extends JpaRepository<UserStoreMembershipEntity, UUID> {

    List<UserStoreMembershipEntity> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndStore_Id(UUID userId, UUID storeId);
}
