package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreProfileJpaRepository extends JpaRepository<StoreProfileEntity, UUID> {

    Optional<StoreProfileEntity> findByGooglePlaceId(String googlePlaceId);
}
