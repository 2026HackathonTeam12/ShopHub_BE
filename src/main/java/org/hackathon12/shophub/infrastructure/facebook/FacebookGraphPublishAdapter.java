package org.hackathon12.shophub.infrastructure.facebook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hackathon12.shophub.domain.facebook.port.FacebookPublishPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FacebookGraphPublishAdapter implements FacebookPublishPort {

    private final RestClient restClient;
    private final FacebookGraphProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FacebookGraphPublishAdapter(RestClient.Builder restClientBuilder, FacebookGraphProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public void ensurePublishReady() {
        validateCredentials();
    }

    @Override
    public String publishPost(String message, List<String> imageUrls) {
        ensurePublishReady();
        validateImages(imageUrls);

        String pageId = resolvePageId();
        if (imageUrls.size() == 1) {
            Map<String, Object> response = post(
                    "/" + pageId + "/photos",
                    Map.of(
                            "url", imageUrls.get(0),
                            "message", message
                    )
            );
            return valueAsString(response.get("id"), "Facebook 게시 ID가 없습니다.");
        }

        List<String> photoIds = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            Map<String, Object> response = post(
                    "/" + pageId + "/photos",
                    Map.of(
                            "url", imageUrl,
                            "published", "false"
                    )
            );
            photoIds.add(valueAsString(response.get("id"), "Facebook 사진 ID가 없습니다."));
        }

        Map<String, String> feedParams = new LinkedHashMap<>();
        feedParams.put("message", message);
        feedParams.put("attached_media", buildAttachedMediaJson(photoIds));

        Map<String, Object> feedResponse = post("/" + pageId + "/feed", feedParams);
        return valueAsString(feedResponse.get("id"), "Facebook 게시 ID가 없습니다.");
    }

    private String buildAttachedMediaJson(List<String> photoIds) {
        String items = photoIds.stream()
                .map(photoId -> "{\"media_fbid\":\"" + photoId + "\"}")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return "[" + items + "]";
    }

    private String resolvePageId() {
        if (StringUtils.hasText(properties.pageId())) {
            return properties.pageId();
        }

        Map<String, Object> response = get("/me", Map.of("fields", "id,name"));
        String pageId = valueAsString(response.get("id"), "Facebook Page ID를 확인할 수 없습니다.");
        if (StringUtils.hasText(properties.allowedPageId()) && !properties.allowedPageId().equals(pageId)) {
            throw new IllegalArgumentException(
                    "Facebook 게시는 허용된 페이지만 사용할 수 있습니다. pageId=" + properties.allowedPageId()
            );
        }
        return pageId;
    }

    private Map<String, Object> post(String endpoint, Map<String, String> params) {
        try {
            String raw = restClient.post()
                    .uri(uriBuilder -> buildUri(uriBuilder.path(endpoint), params))
                    .retrieve()
                    .body(String.class);
            return parseResponse(raw);
        } catch (FacebookGraphApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FacebookGraphApiException("Facebook Graph API 호출 실패: " + exception.getMessage());
        }
    }

    private Map<String, Object> get(String endpoint, Map<String, String> params) {
        try {
            String raw = restClient.get()
                    .uri(uriBuilder -> buildUri(uriBuilder.path(endpoint), params))
                    .retrieve()
                    .body(String.class);
            return parseResponse(raw);
        } catch (FacebookGraphApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FacebookGraphApiException("Facebook Graph API 호출 실패: " + exception.getMessage());
        }
    }

    private Map<String, Object> parseResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new FacebookGraphApiException("Facebook Graph API 응답이 비어 있습니다.");
        }
        try {
            Map<String, Object> body = objectMapper.readValue(raw, new TypeReference<>() {
            });
            if (body.containsKey("error")) {
                throw new FacebookGraphApiException("Facebook Graph API 오류: " + body.get("error"));
            }
            return body;
        } catch (FacebookGraphApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FacebookGraphApiException("Facebook Graph API 응답 파싱 실패: " + raw);
        }
    }

    private java.net.URI buildUri(org.springframework.web.util.UriBuilder builder, Map<String, String> params) {
        builder = builder.queryParam("access_token", properties.accessToken());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder = builder.queryParam(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(properties.accessToken())) {
            throw new IllegalArgumentException("Facebook Graph API Page Access Token이 설정되지 않았습니다.");
        }
        if (StringUtils.hasText(properties.pageId())
                && StringUtils.hasText(properties.allowedPageId())
                && !properties.allowedPageId().equals(properties.pageId())) {
            throw new IllegalArgumentException(
                    "Facebook 게시는 허용된 페이지만 사용할 수 있습니다."
            );
        }
    }

    private void validateImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Facebook 게시에는 최소 1개 이상의 이미지가 필요합니다.");
        }
        if (imageUrls.size() > 10) {
            throw new IllegalArgumentException("Facebook 게시 이미지는 최대 10장까지 지원합니다.");
        }
    }

    private String valueAsString(Object value, String messageIfNull) {
        if (value == null) {
            throw new FacebookGraphApiException(messageIfNull);
        }
        return String.valueOf(value);
    }
}
