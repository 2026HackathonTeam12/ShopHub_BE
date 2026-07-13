package org.hackathon12.shophub.infrastructure.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlaceDetailsResponse(
        String status,
        String error_message,
        Result result
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            String place_id,
            String name,
            String url,
            Double rating,
            Integer user_ratings_total,
            List<Review> reviews
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(
            String author_name,
            Integer rating,
            String text,
            Long time
    ) {
    }
}
