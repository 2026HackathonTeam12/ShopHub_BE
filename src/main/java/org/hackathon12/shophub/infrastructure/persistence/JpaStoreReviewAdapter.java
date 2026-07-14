package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.StoreReviewPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    @Transactional
    public MergeResult mergeFromSource(UUID storeId, List<StoreReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return new MergeResult(0, 0);
        }

        int newReviews = 0;
        int updatedReviews = 0;
        for (StoreReview incoming : reviews) {
            if (!StringUtils.hasText(incoming.sourceReviewId())) {
                continue;
            }

            StoreReviewEntity existing = storeReviewJpaRepository
                    .findByStore_IdAndSourceReviewId(storeId, incoming.sourceReviewId())
                    .orElse(null);

            if (existing == null) {
                storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(incoming));
                newReviews++;
                continue;
            }

            StoreReview current = existing.toDomain();
            storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(new StoreReview(
                    current.id(),
                    current.storeId(),
                    incoming.platform(),
                    incoming.sourceReviewId(),
                    incoming.authorName(),
                    incoming.rating(),
                    incoming.content(),
                    incoming.reviewedAt(),
                    current.reply(),
                    current.repliedAt()
            )));
            updatedReviews++;
        }

        return new MergeResult(newReviews, updatedReviews);
    }

    @Override
    public StoreReview save(StoreReview storeReview) {
        StoreReviewEntity saved = storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(storeReview));
        return saved.toDomain();
    }
}
