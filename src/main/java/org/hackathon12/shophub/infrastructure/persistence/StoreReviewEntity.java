package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.review.model.StoreReview;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_reviews")
public class StoreReviewEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "store_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_store_reviews_store")
    )
    private StoreProfileEntity store;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false)
    private int rating;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Instant reviewedAt;

    @Lob
    private String reply;

    private Instant repliedAt;

    protected StoreReviewEntity() {
    }

    public static StoreReviewEntity fromDomain(StoreReview storeReview) {
        StoreReviewEntity entity = new StoreReviewEntity();
        entity.id = storeReview.id();
        entity.store = StoreProfileEntity.reference(storeReview.storeId());
        entity.platform = storeReview.platform();
        entity.authorName = storeReview.authorName();
        entity.rating = storeReview.rating();
        entity.content = storeReview.content();
        entity.reviewedAt = storeReview.reviewedAt();
        entity.reply = storeReview.reply();
        entity.repliedAt = storeReview.repliedAt();
        return entity;
    }

    public StoreReview toDomain() {
        return new StoreReview(
                id,
                store.getId(),
                platform,
                authorName,
                rating,
                content,
                reviewedAt,
                reply,
                repliedAt
        );
    }
}
