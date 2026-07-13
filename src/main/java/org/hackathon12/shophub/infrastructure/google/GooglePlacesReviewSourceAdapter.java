package org.hackathon12.shophub.infrastructure.google;

import org.hackathon12.shophub.domain.review.model.ReviewSourceData;
import org.hackathon12.shophub.domain.review.port.ReviewSourcePort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class GooglePlacesReviewSourceAdapter implements ReviewSourcePort {

    private static final String SOURCE_PLATFORM = "GOOGLE_MAPS";

    private final RestClient restClient;
    private final GooglePlacesProperties properties;

    public GooglePlacesReviewSourceAdapter(RestClient.Builder restClientBuilder, GooglePlacesProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public ReviewSourceData fetchByPlaceId(String placeId) {
        validateApiKey();

        GooglePlaceDetailsResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("language", properties.language())
                        .queryParam("reviews_sort", "newest")
                        .queryParam("key", properties.apiKey())
                        .build())
                .retrieve()
                .body(GooglePlaceDetailsResponse.class);

        if (response == null) {
            throw new GooglePlacesApiException("Google Places API 응답이 비어 있습니다.");
        }
        if (!"OK".equals(response.status())) {
            String message = response.error_message() == null ? response.status() : response.error_message();
            throw new GooglePlacesApiException("Google Places API 오류: " + message);
        }

        GooglePlaceDetailsResponse.Result placeResult = response.result();
        if (placeResult == null) {
            throw new GooglePlacesApiException("Google Places API place 상세 데이터가 없습니다.");
        }

        List<ReviewSourceData.ReviewItem> reviews = new ArrayList<>();
        if (placeResult.reviews() != null) {
            for (GooglePlaceDetailsResponse.Review review : placeResult.reviews()) {
                reviews.add(new ReviewSourceData.ReviewItem(
                        review.author_name(),
                        review.rating() == null ? 1 : review.rating(),
                        review.text(),
                        epochSecondsToInstant(review.time())
                ));
            }
        }

        return new ReviewSourceData(
                SOURCE_PLATFORM,
                placeId,
                placeResult.name(),
                placeResult.rating(),
                placeResult.user_ratings_total(),
                placeResult.url(),
                reviews
        );
    }

    private Instant epochSecondsToInstant(Long epochSeconds) {
        if (epochSeconds == null || epochSeconds <= 0) {
            return Instant.EPOCH;
        }
        return Instant.ofEpochSecond(epochSeconds);
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new GooglePlacesApiException("Google Places API 키가 설정되지 않았습니다. GOOGLE_PLACES_API_KEY 환경 변수를 설정해 주세요.");
        }
    }
}
