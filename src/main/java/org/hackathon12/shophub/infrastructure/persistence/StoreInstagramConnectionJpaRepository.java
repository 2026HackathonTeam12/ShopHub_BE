package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreInstagramConnectionJpaRepository extends JpaRepository<StoreInstagramConnectionEntity, UUID> {

    Optional<StoreInstagramConnectionEntity> findByStore_Id(UUID storeId);
}
