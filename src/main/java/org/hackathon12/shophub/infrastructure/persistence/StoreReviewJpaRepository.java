package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreReviewJpaRepository extends JpaRepository<StoreReviewEntity, UUID> {

    List<StoreReviewEntity> findByStore_Id(UUID storeId);

    void deleteByStore_Id(UUID storeId);
}
