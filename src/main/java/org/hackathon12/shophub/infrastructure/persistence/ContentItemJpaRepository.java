package org.hackathon12.shophub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentItemJpaRepository extends JpaRepository<ContentItemEntity, UUID> {

    List<ContentItemEntity> findByStore_IdOrderByUpdatedAtDesc(UUID storeId);
}
