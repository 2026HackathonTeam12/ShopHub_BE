package org.hackathon12.shophub.domain.store.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoreProfile(
        UUID id,
        String name,
        String phone,
        String introduction,
        String address,
        String category,
        String toneOfVoice,
        List<BusinessHour> businessHours,
        List<MenuItem> menuItems,
        String googlePlaceId,
        String googleReviewUrl,
        int googleTotalReviews,
        Instant updatedAt
) {
}
