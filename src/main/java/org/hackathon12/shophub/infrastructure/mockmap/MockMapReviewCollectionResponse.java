package org.hackathon12.shophub.infrastructure.mockmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MockMapReviewCollectionResponse(
        List<ReviewItem> data,
        String place_id,
        String error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewItem(
            Integer id,
            String place_id,
            String place_name,
            String author_name,
            Integer rating,
            String content,
            String created_at,
            List<ReplyItem> replies
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReplyItem(
            Integer id,
            String content
    ) {
    }
}
