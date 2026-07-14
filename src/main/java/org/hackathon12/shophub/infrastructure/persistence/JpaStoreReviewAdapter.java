package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.port.StoreReviewPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        List<StoreReview> incoming = reviews == null ? List.of() : reviews;
        Set<String> incomingSourceIds = new HashSet<>();

        int newReviews = 0;
        int updatedReviews = 0;
        for (StoreReview review : incoming) {
            if (!StringUtils.hasText(review.sourceReviewId())) {
                continue;
            }
            incomingSourceIds.add(review.sourceReviewId());

            StoreReviewEntity existing = storeReviewJpaRepository
                    .findByStore_IdAndSourceReviewId(storeId, review.sourceReviewId())
                    .orElse(null);

            if (existing == null) {
                storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(review));
                newReviews++;
                continue;
            }

            StoreReview current = existing.toDomain();
            storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(new StoreReview(
                    current.id(),
                    current.storeId(),
                    review.platform(),
                    review.sourceReviewId(),
                    review.authorName(),
                    review.rating(),
                    review.content(),
                    review.reviewedAt(),
                    current.reply(),
                    current.repliedAt()
            )));
            updatedReviews++;
        }

        // Drop reviews that no longer belong to this store's current place.
        for (StoreReviewEntity existing : storeReviewJpaRepository.findByStore_Id(storeId)) {
            String sourceReviewId = existing.toDomain().sourceReviewId();
            if (!StringUtils.hasText(sourceReviewId) || !incomingSourceIds.contains(sourceReviewId)) {
                storeReviewJpaRepository.delete(existing);
            }
        }

        return new MergeResult(newReviews, updatedReviews);
    }

    @Override
    public StoreReview save(StoreReview storeReview) {
        StoreReviewEntity saved = storeReviewJpaRepository.save(StoreReviewEntity.fromDomain(storeReview));
        return saved.toDomain();
    }
}
