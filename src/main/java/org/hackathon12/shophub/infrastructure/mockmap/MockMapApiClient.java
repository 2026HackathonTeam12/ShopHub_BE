package org.hackathon12.shophub.infrastructure.mockmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class MockMapApiClient {

    private final RestClient restClient;
    private final MockMapApiProperties apiProperties;
    private final MockMapOAuthProperties oauthProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockMapApiClient(
            RestClient.Builder restClientBuilder,
            MockMapApiProperties apiProperties,
            MockMapOAuthProperties oauthProperties
    ) {
        this.restClient = restClientBuilder.baseUrl(apiProperties.baseUrl()).build();
        this.apiProperties = apiProperties;
        this.oauthProperties = oauthProperties;
    }

    public MockMapReviewCollectionResponse listReviews(String placeId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(resolveReviewsPath())
                            .queryParam("place_id", placeId)
                            .build())
                    .retrieve()
                    .body(MockMapReviewCollectionResponse.class);
        } catch (RestClientResponseException exception) {
            throw toApiException("리뷰 목록 조회", exception);
        } catch (Exception exception) {
            throw new MockMapApiException("ShopHub_MockMap API 리뷰 목록 조회에 실패했습니다.", exception);
        }
    }

    public MockMapOAuthTokenResponse exchangeAuthorizationCode(
            String clientId,
            String clientSecret,
            String code,
            String redirectUri,
            String state
    ) {
        try {
            return restClient.post()
                    .uri(resolvePath(apiProperties.oauthTokenPath(), "/oauth/token/"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(Map.of(
                            "grant_type", "authorization_code",
                            "client_id", clientId,
                            "client_secret", clientSecret,
                            "code", code,
                            "redirect_uri", redirectUri,
                            "state", state == null ? "" : state
                    )))
                    .retrieve()
                    .body(MockMapOAuthTokenResponse.class);
        } catch (RestClientResponseException exception) {
            throw toApiException("OAuth authorization code 교환", exception);
        } catch (Exception exception) {
            throw new MockMapApiException("ShopHub_MockMap API OAuth authorization code 교환에 실패했습니다.", exception);
        }
    }

    public MockMapOAuthTokenResponse refreshOwnerToken(
            String clientId,
            String clientSecret,
            String refreshToken
    ) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "refresh_token");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("refresh_token", refreshToken);

            return restClient.post()
                    .uri(resolvePath(apiProperties.oauthTokenPath(), "/oauth/token/"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MockMapOAuthTokenResponse.class);
        } catch (RestClientResponseException exception) {
            throw toApiException("OAuth 토큰 갱신", exception);
        } catch (Exception exception) {
            throw new MockMapApiException("ShopHub_MockMap API OAuth 토큰 갱신에 실패했습니다.", exception);
        }
    }

    public void revokeToken(String clientId, String clientSecret, String token) {
        try {
            restClient.post()
                    .uri(resolvePath(oauthProperties.revokePath(), "/oauth/revoke/"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(Map.of(
                            "client_id", clientId,
                            "client_secret", clientSecret,
                            "token", token
                    )))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw toApiException("OAuth 토큰 폐기", exception);
        } catch (Exception exception) {
            throw new MockMapApiException("ShopHub_MockMap API OAuth 토큰 폐기에 실패했습니다.", exception);
        }
    }

    public MockMapReviewSingleResponse createReply(String reviewId, String accessToken, String content) {
        try {
            String jsonBody = objectMapper.writeValueAsString(Map.of("content", content));
            return restClient.post()
                    .uri(resolveReplyPath(reviewId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(jsonBody)
                    .retrieve()
                    .body(MockMapReviewSingleResponse.class);
        } catch (RestClientResponseException exception) {
            throw toApiException("답글 작성", exception);
        } catch (Exception exception) {
            throw new MockMapApiException("ShopHub_MockMap API 답글 작성에 실패했습니다.", exception);
        }
    }

    public boolean isHealthy() {
        try {
            JsonNode response = restClient.get()
                    .uri(resolvePath(apiProperties.healthPath(), "/health/"))
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && "ok".equalsIgnoreCase(response.path("status").asText());
        } catch (Exception ignored) {
            return false;
        }
    }

    private MockMapApiException toApiException(String operation, RestClientResponseException exception) {
        String detail = extractErrorMessage(exception.getResponseBodyAsString());
        String message = StringUtils.hasText(detail)
                ? "ShopHub_MockMap API " + operation + " 실패: " + detail
                : "ShopHub_MockMap API " + operation + " 실패 (HTTP " + exception.getStatusCode().value() + ")";
        return new MockMapApiException(message, exception);
    }

    private String extractErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.hasNonNull("error")) {
                return jsonNode.get("error").asText();
            }
            if (jsonNode.hasNonNull("errors")) {
                return jsonNode.get("errors").toString();
            }
            return responseBody;
        } catch (Exception ignored) {
            return responseBody;
        }
    }

    private String resolvePath(String configuredPath, String defaultPath) {
        String path = StringUtils.hasText(configuredPath) ? configuredPath : defaultPath;
        return path.startsWith("/") ? path : "/" + path;
    }

    private String resolveReviewsPath() {
        return resolvePath(apiProperties.reviewsPath(), "/api/reviews/");
    }

    private String resolveReplyPath(String reviewId) {
        String reviewsPath = resolveReviewsPath();
        String normalizedReviewsPath = reviewsPath.endsWith("/") ? reviewsPath : reviewsPath + "/";
        return normalizedReviewsPath + reviewId + "/reply/";
    }
}
