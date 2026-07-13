package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.StoreReviewPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class JpaStoreReviewAdapter implements StoreReviewPort {

    private final StoreReviewJpaRepository storeReviewJpaRepository;

    public JpaStoreReviewAdapter(StoreReviewJpaRepository storeReviewJpaRepository) {
        this.storeReviewJpaRepository = storeReviewJpaRepository;
    }

    @Override
    public List<StoreReview> findByStoreId(UUID storeId) {
        return storeReviewJpaRepository.findByStore_Id(storeId).stream()
                .map(StoreReviewEntity::toDomain)
                .toList();
    }

    @Override
    public StoreReview findById(UUID reviewId) {
        return storeReviewJpaRepository.findById(reviewId)
                .map(StoreReviewEntity::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional
    public void replaceByStoreId(UUID storeId, List<StoreReview> reviews) {
        storeReviewJpaRepository.deleteByStore_Id(storeId);
        if (reviews == null || reviews.isEmpty()) {
            return;
        }
        List<StoreReviewEntity> entities = reviews.stream()
                .map(StoreReviewEntity::fromDomain)
                .toList();
        storeReviewJpaRepository.saveAll(entities);
    }

    @Override
    public StoreReview save(StoreReview storeReview) {
        StoreReviewEntity saved = storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(storeReview));
        return saved.toDomain();
    }
}
