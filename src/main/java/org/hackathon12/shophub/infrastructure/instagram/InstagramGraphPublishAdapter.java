package org.hackathon12.shophub.infrastructure.instagram;

import org.hackathon12.shophub.domain.instagram.port.InstagramPublishPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InstagramGraphPublishAdapter implements InstagramPublishPort {

    private final RestClient restClient;
    private final InstagramGraphProperties properties;

    public InstagramGraphPublishAdapter(RestClient.Builder restClientBuilder, InstagramGraphProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public String publishPost(String caption, List<String> imageUrls) {
        validateCredentials();
        validateImages(imageUrls);

        if (imageUrls.size() == 1) {
            String singleContainerId = createSingleImageContainer(caption, imageUrls.get(0));
            waitUntilContainerReady(singleContainerId);
            return publishContainer(singleContainerId);
        }

        List<String> childIds = imageUrls.stream()
                .map(this::createCarouselItemContainer)
                .toList();
        String carouselContainerId = createCarouselContainer(caption, childIds);

        waitUntilContainerReady(carouselContainerId);
        return publishContainer(carouselContainerId);
    }

    private String createSingleImageContainer(String caption, String imageUrl) {
        Map<String, Object> response = request(
                "/" + properties.accountId() + "/media",
                Map.of(
                        "image_url", imageUrl,
                        "caption", caption
                ),
                "POST"
        );
        return valueAsString(response.get("id"), "단일 이미지 컨테이너 ID가 없습니다.");
    }

    private String createCarouselItemContainer(String imageUrl) {
        Map<String, Object> response = request(
                "/" + properties.accountId() + "/media",
                Map.of(
                        "image_url", imageUrl,
                        "is_carousel_item", "true"
                ),
                "POST"
        );
        return valueAsString(response.get("id"), "캐러셀 아이템 컨테이너 ID가 없습니다.");
    }

    private String createCarouselContainer(String caption, List<String> childIds) {
        Map<String, Object> response = request(
                "/" + properties.accountId() + "/media",
                Map.of(
                        "media_type", "CAROUSEL",
                        "children", childIds.stream().collect(Collectors.joining(",")),
                        "caption", caption
                ),
                "POST"
        );
        return valueAsString(response.get("id"), "캐러셀 컨테이너 ID가 없습니다.");
    }

    private void waitUntilContainerReady(String creationId) {
        int maxAttempts = properties.maxAttempts() == null ? 15 : properties.maxAttempts();
        long intervalMs = properties.intervalMs() == null ? 2_000L : properties.intervalMs();

        for (int i = 0; i < maxAttempts; i++) {
            Map<String, Object> statusResponse = request(
                    "/" + creationId,
                    Map.of("fields", "status_code"),
                    "GET"
            );
            String statusCode = valueAsString(statusResponse.get("status_code"), "status_code 값이 없습니다.");

            if ("FINISHED".equals(statusCode)) {
                return;
            }
            if ("ERROR".equals(statusCode) || "EXPIRED".equals(statusCode)) {
                throw new InstagramGraphApiException("Instagram 컨테이너 처리 실패: " + statusCode);
            }

            sleep(intervalMs);
        }
        throw new InstagramGraphApiException("Instagram 컨테이너가 시간 내 FINISHED 상태가 되지 않았습니다.");
    }

    private String publishContainer(String creationId) {
        Map<String, Object> response = request(
                "/" + properties.accountId() + "/media_publish",
                Map.of("creation_id", creationId),
                "POST"
        );
        return valueAsString(response.get("id"), "게시 결과 Media ID가 없습니다.");
    }

    private Map<String, Object> request(String endpoint, Map<String, String> params, String method) {
        try {
            Map<String, Object> body;
            if ("POST".equals(method)) {
                body = restClient.post()
                        .uri(uriBuilder -> buildUri(uriBuilder.path(endpoint), params))
                        .retrieve()
                        .body(Map.class);
            } else {
                body = restClient.get()
                        .uri(uriBuilder -> buildUri(uriBuilder.path(endpoint), params))
                        .retrieve()
                        .body(Map.class);
            }

            if (body == null) {
                throw new InstagramGraphApiException("Instagram Graph API 응답이 비어 있습니다.");
            }
            return body;
        } catch (Exception exception) {
            throw new InstagramGraphApiException("Instagram Graph API 호출 실패: " + exception.getMessage());
        }
    }

    private java.net.URI buildUri(org.springframework.web.util.UriBuilder builder, Map<String, String> params) {
        builder = builder.queryParam("access_token", properties.accessToken());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder = builder.queryParam(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private String valueAsString(Object value, String messageIfNull) {
        if (value == null) {
            throw new InstagramGraphApiException(messageIfNull);
        }
        return String.valueOf(value);
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(properties.accountId()) || !StringUtils.hasText(properties.accessToken())) {
            throw new IllegalArgumentException("Instagram Graph API 계정/토큰이 설정되지 않았습니다.");
        }
    }

    private void validateImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Instagram 게시에는 최소 1개 이상의 이미지가 필요합니다.");
        }
        if (imageUrls.size() > 10) {
            throw new IllegalArgumentException("Instagram 게시 이미지는 최대 10장까지 지원합니다.");
        }
    }

    private void sleep(long intervalMs) {
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new InstagramGraphApiException("Instagram 게시 대기 중 인터럽트가 발생했습니다.");
        }
    }
}
