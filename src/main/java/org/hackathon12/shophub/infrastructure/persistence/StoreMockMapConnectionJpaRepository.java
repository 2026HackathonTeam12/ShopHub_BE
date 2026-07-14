package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreMockMapConnectionJpaRepository extends JpaRepository<StoreMockMapConnectionEntity, UUID> {

    Optional<StoreMockMapConnectionEntity> findByStore_Id(UUID storeId);
}
