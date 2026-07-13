package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.port.StoreProfilePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaStoreProfileAdapter implements StoreProfilePort {

    private final StoreProfileJpaRepository storeProfileJpaRepository;

    public JpaStoreProfileAdapter(StoreProfileJpaRepository storeProfileJpaRepository) {
        this.storeProfileJpaRepository = storeProfileJpaRepository;
    }

    @Override
    public List<StoreProfile> findAll() {
        return storeProfileJpaRepository.findAll().stream()
                .map(StoreProfileEntity::toDomain)
                .toList();
    }

    @Override
    public StoreProfile findById(UUID storeId) {
        return storeProfileJpaRepository.findById(storeId)
                .map(StoreProfileEntity::toDomain)
                .orElse(null);
    }

    @Override
    public StoreProfile save(StoreProfile storeProfile) {
        StoreProfileEntity entity = storeProfileJpaRepository.findById(storeProfile.id())
                .orElseGet(StoreProfileEntity::new);
        entity.applyDomain(storeProfile);
        return storeProfileJpaRepository.save(entity).toDomain();
    }
}
