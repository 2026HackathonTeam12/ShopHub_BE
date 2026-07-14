package org.hackathon12.shophub.infrastructure.mockmap;

import org.hackathon12.shophub.domain.review.model.ReviewSourceData;
import org.hackathon12.shophub.domain.review.port.ReviewSourcePort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class MockMapReviewSourceAdapter implements ReviewSourcePort {

    private static final String SOURCE_PLATFORM = "MOCK_MAP";

    private final MockMapApiClient mockMapApiClient;
    private final MockMapApiProperties properties;

    public MockMapReviewSourceAdapter(MockMapApiClient mockMapApiClient, MockMapApiProperties properties) {
        this.mockMapApiClient = mockMapApiClient;
        this.properties = properties;
    }

    @Override
    public ReviewSourceData fetchByPlaceId(String placeId) {
        MockMapReviewCollectionResponse response = mockMapApiClient.listReviews(placeId);

        if (response == null) {
            throw new MockMapApiException("ShopHub_MockMap API 응답이 비어 있습니다.");
        }
        if (StringUtils.hasText(response.error())) {
            throw new MockMapApiException("ShopHub_MockMap API 오류: " + response.error());
        }

        List<MockMapReviewCollectionResponse.ReviewItem> sourceReviews = response.data() == null ? List.of() : response.data();
        List<ReviewSourceData.ReviewItem> reviews = sourceReviews.stream()
                .map(this::toReviewItem)
                .toList();

        String storeName = resolveStoreName(sourceReviews, placeId);
        Double placeRating = calculateAverageRating(reviews);
        Integer placeRatingCount = reviews.size();

        return new ReviewSourceData(
                SOURCE_PLATFORM,
                placeId,
                storeName,
                placeRating,
                placeRatingCount,
                buildPlaceUrl(placeId),
                reviews
        );
    }

    private String resolveStoreName(List<MockMapReviewCollectionResponse.ReviewItem> sourceReviews, String defaultName) {
        for (MockMapReviewCollectionResponse.ReviewItem review : sourceReviews) {
            if (StringUtils.hasText(review.place_name())) {
                return review.place_name();
            }
        }
        return defaultName;
    }

    private ReviewSourceData.ReviewItem toReviewItem(MockMapReviewCollectionResponse.ReviewItem review) {
        return new ReviewSourceData.ReviewItem(
                review.id() == null ? null : String.valueOf(review.id()),
                StringUtils.hasText(review.author_name()) ? review.author_name() : "익명",
                normalizeRating(review.rating()),
                review.content() == null ? "" : review.content(),
                parseInstant(review.created_at())
        );
    }

    private int normalizeRating(Integer rating) {
        if (rating == null) {
            return 1;
        }
        if (rating < 1) {
            return 1;
        }
        return Math.min(rating, 5);
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return Instant.EPOCH;
        }
    }

    private Double calculateAverageRating(List<ReviewSourceData.ReviewItem> reviews) {
        if (reviews.isEmpty()) {
            return null;
        }
        return reviews.stream()
                .mapToInt(ReviewSourceData.ReviewItem::rating)
                .average()
                .orElse(0.0);
    }

    private String buildPlaceUrl(String placeId) {
        if (!StringUtils.hasText(properties.baseUrl())) {
            return null;
        }
        String encodedPlaceId = URLEncoder.encode(placeId, StandardCharsets.UTF_8);
        return properties.baseUrl().replaceAll("/+$", "") + "/?place_id=" + encodedPlaceId;
    }
}
