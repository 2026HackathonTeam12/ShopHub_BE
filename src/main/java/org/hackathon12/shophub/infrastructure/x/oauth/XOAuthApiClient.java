package org.hackathon12.shophub.infrastructure.x.oauth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hackathon12.shophub.infrastructure.x.XApiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class XOAuthApiClient {

    private static final String USERS_ME_URL = "https://api.x.com/2/users/me";

    private final RestClient restClient;
    private final XOAuthProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XOAuthApiClient(RestClient.Builder restClientBuilder, XOAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public XOAuthTokenResponse exchangeAuthorizationCode(String code, String codeVerifier) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", properties.redirectUri());
        body.add("code_verifier", codeVerifier);

        return postToken(body);
    }

    public XOAuthTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        return postToken(body);
    }

    public Map<String, Object> getCurrentUser(String accessToken) {
        try {
            String raw = restClient.get()
                    .uri(USERS_ME_URL + "?user.fields=username")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            return parseJsonMap(raw);
        } catch (XApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new XApiException("X 사용자 정보 조회 실패: " + exception.getMessage());
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", refreshToken);
        body.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(properties.revokeUrl())
                    .header(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new XApiException("X OAuth revoke 실패: " + exception.getMessage());
        }
    }

    private XOAuthTokenResponse postToken(MultiValueMap<String, String> body) {
        try {
            String raw = restClient.post()
                    .uri(properties.tokenUrl())
                    .header(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> parsed = parseJsonMap(raw);
            if (parsed.containsKey("error")) {
                throw new XApiException("X OAuth 토큰 오류: " + parsed.get("error"));
            }

            return objectMapper.convertValue(parsed, XOAuthTokenResponse.class);
        } catch (XApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new XApiException("X OAuth 토큰 요청 실패: " + exception.getMessage());
        }
    }

    private String basicAuthorizationHeader() {
        String raw = properties.clientId() + ":" + properties.clientSecret();
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private Map<String, Object> parseJsonMap(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new XApiException("X OAuth 응답이 비어 있습니다.");
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new XApiException("X OAuth 응답 파싱 실패: " + raw);
        }
    }
}
