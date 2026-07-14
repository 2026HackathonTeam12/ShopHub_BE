package org.hackathon12.shophub.infrastructure.mockmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MockMapReviewSingleResponse(
        MockMapReviewCollectionResponse.ReviewItem data,
        String error
) {
}
