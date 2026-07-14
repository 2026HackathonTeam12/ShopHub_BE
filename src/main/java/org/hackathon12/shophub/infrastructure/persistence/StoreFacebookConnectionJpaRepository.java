package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreFacebookConnectionJpaRepository extends JpaRepository<StoreFacebookConnectionEntity, UUID> {

    Optional<StoreFacebookConnectionEntity> findByStore_Id(UUID storeId);
}
