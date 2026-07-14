package org.hackathon12.shophub.infrastructure.facebook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hackathon12.shophub.domain.facebook.port.FacebookPublishPort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        verifyPageAccessToken();
    }

    @Override
    public String publishPost(String message, List<String> imageUrls) {
        ensurePublishReady();
        List<String> urls = imageUrls == null ? List.of() : imageUrls;
        validateImages(urls);

        String pageId = resolvePageId();
        if (urls.isEmpty()) {
            Map<String, Object> feedResponse = post("/" + pageId + "/feed", Map.of("message", message));
            return valueAsString(feedResponse.get("id"), "Facebook 게시 ID가 없습니다.");
        }
        if (urls.size() == 1) {
            Map<String, Object> response = post(
                    "/" + pageId + "/photos",
                    Map.of(
                            "url", urls.get(0),
                            "message", message
                    )
            );
            return valueAsString(response.get("id"), "Facebook 게시 ID가 없습니다.");
        }

        List<String> photoIds = new ArrayList<>();
        for (String imageUrl : urls) {
            Map<String, Object> response = post(
                    "/" + pageId + "/photos",
                    Map.of(
                            "url", imageUrl,
                            "published", "false"
                    )
            );
            photoIds.add(valueAsString(response.get("id"), "Facebook 사진 ID가 없습니다."));
        }

        Map<String, String> feedParams = buildFeedParams(message, photoIds);
        Map<String, Object> feedResponse = post("/" + pageId + "/feed", feedParams);
        return valueAsString(feedResponse.get("id"), "Facebook 게시 ID가 없습니다.");
    }

    Map<String, String> buildFeedParams(String message, List<String> photoIds) {
        Map<String, String> feedParams = new LinkedHashMap<>();
        feedParams.put("message", message);
        for (int index = 0; index < photoIds.size(); index++) {
            feedParams.put("attached_media[" + index + "]", toAttachedMediaValue(photoIds.get(index)));
        }
        return feedParams;
    }

    private String toAttachedMediaValue(String photoId) {
        try {
            return objectMapper.writeValueAsString(Map.of("media_fbid", photoId));
        } catch (JsonProcessingException exception) {
            throw new FacebookGraphApiException("Facebook attached_media JSON 생성 실패: " + exception.getMessage());
        }
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

    private void verifyPageAccessToken() {
        Map<String, Object> response = get("/debug_token", Map.of(
                "input_token", properties.accessToken()
        ));
        Object dataObject = response.get("data");
        if (!(dataObject instanceof Map<?, ?> data)) {
            throw new IllegalArgumentException("Facebook Page Access Token을 확인할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(data.get("is_valid"))) {
            throw new IllegalArgumentException("Facebook Page Access Token이 유효하지 않거나 만료되었습니다.");
        }
        if (!"PAGE".equals(String.valueOf(data.get("type")))) {
            throw new IllegalArgumentException(
                    "Facebook 게시에는 PAGE Access Token이 필요합니다. USER 토큰이 아닌 페이지 토큰을 설정하세요."
            );
        }
    }

    private Map<String, Object> post(String endpoint, Map<String, String> params) {
        try {
            String raw = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path(endpoint)
                            .queryParam("access_token", properties.accessToken())
                            .build())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(buildFormBody(params))
                    .retrieve()
                    .body(String.class);
            return parseResponse(raw);
        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
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
        } catch (RestClientResponseException exception) {
            throw toApiException(exception);
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

    private FacebookGraphApiException toApiException(RestClientResponseException exception) {
        String detail = extractErrorMessage(exception.getResponseBodyAsString());
        if (StringUtils.hasText(detail)) {
            return new FacebookGraphApiException("Facebook Graph API 오류: " + detail);
        }
        return new FacebookGraphApiException(
                "Facebook Graph API 호출 실패 (HTTP " + exception.getStatusCode().value() + ")"
        );
    }

    private String extractErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.get("error");
            if (error == null || error.isNull()) {
                return responseBody;
            }
            if (error.hasNonNull("message")) {
                return error.get("message").asText();
            }
            return error.toString();
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private java.net.URI buildUri(org.springframework.web.util.UriBuilder builder, Map<String, String> params) {
        builder = builder.queryParam("access_token", properties.accessToken());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder = builder.queryParam(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private String buildFormBody(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
