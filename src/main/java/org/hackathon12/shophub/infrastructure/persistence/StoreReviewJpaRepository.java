package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreReviewJpaRepository extends JpaRepository<StoreReviewEntity, UUID> {

    List<StoreReviewEntity> findByStore_Id(UUID storeId);

    Optional<StoreReviewEntity> findByStore_IdAndSourceReviewId(UUID storeId, String sourceReviewId);

    void deleteByStore_Id(UUID storeId);
}
