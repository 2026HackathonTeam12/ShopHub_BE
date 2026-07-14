package org.hackathon12.shophub.infrastructure.x;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class XApiClient {

    private final RestClient restClient;
    private final XApiProperties properties;

    public XApiClient(RestClient.Builder restClientBuilder, XApiProperties properties) {
        this.restClient = restClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT, "ShopHub/1.0")
                .build();
        this.properties = properties;
    }

    public String publishTweet(String accessToken, String text, List<String> mediaIds) {
        Map<String, Object> body = mediaIds == null || mediaIds.isEmpty()
                ? Map.of("text", text)
                : Map.of(
                        "text", text,
                        "media", Map.of("media_ids", mediaIds)
                );

        Map<String, Object> response = authorizedJsonPost(accessToken, properties.tweetsUrl(), body);
        Map<String, Object> data = castMap(response.get("data"));
        String tweetId = valueAsString(data.get("id"), "X 게시 ID가 없습니다.");
        if (!StringUtils.hasText(tweetId)) {
            throw new XApiException("X 게시 ID가 비어 있습니다.");
        }
        return tweetId;
    }

    public String uploadImage(String accessToken, byte[] imageBytes, String fileName) {
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("media_category", "tweet_image");
        multipartBody.add("media_type", guessMediaType(fileName));
        multipartBody.add("media", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        try {
            Map<String, Object> response = restClient.post()
                    .uri(properties.mediaUploadUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, clientResponse) -> {
                        throw new XApiException(
                                "X 미디어 업로드 실패: HTTP " + clientResponse.getStatusCode().value()
                                        + readErrorBody(clientResponse)
                        );
                    })
                    .body(Map.class);

            if (response == null) {
                throw new XApiException("X 미디어 업로드 응답이 비어 있습니다.");
            }

            Map<String, Object> data = castMap(response.get("data"));
            String mediaId = valueAsString(data.get("id"), "X media_id가 없습니다.");
            if (!StringUtils.hasText(mediaId)) {
                throw new XApiException("X media_id가 비어 있습니다.");
            }
            return mediaId;
        } catch (XApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new XApiException("X 미디어 업로드 실패: " + exception.getMessage());
        }
    }

    private String guessMediaType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "image/jpeg";
        }
        return switch (fileName.substring(dotIndex + 1).toLowerCase()) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "tif", "tiff" -> "image/tiff";
            default -> "image/jpeg";
        };
    }

    public byte[] downloadImage(String imageUrl) {
        try {
            byte[] bytes = restClient.get()
                    .uri(imageUrl)
                    .header(HttpHeaders.ACCEPT, "image/*,*/*")
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, clientResponse) -> {
                        throw new XApiException(
                                "이미지 다운로드 실패: HTTP " + clientResponse.getStatusCode().value() + " url=" + imageUrl
                        );
                    })
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new XApiException("이미지 다운로드 결과가 비어 있습니다: " + imageUrl);
            }
            return bytes;
        } catch (XApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new XApiException("이미지 다운로드 실패: " + imageUrl + " (" + exception.getMessage() + ")");
        }
    }

    private Map<String, Object> authorizedJsonPost(String accessToken, String url, Map<String, Object> body) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, clientResponse) -> {
                        throw new XApiException(
                                "X API 호출 실패: HTTP " + clientResponse.getStatusCode().value()
                                        + readErrorBody(clientResponse)
                        );
                    })
                    .body(Map.class);

            if (response == null) {
                throw new XApiException("X API 응답이 비어 있습니다.");
            }
            return response;
        } catch (XApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new XApiException("X API 호출 실패: " + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new XApiException("X API 응답 형식이 올바르지 않습니다.");
    }

    private String valueAsString(Object value, String messageIfNull) {
        if (value == null) {
            throw new XApiException(messageIfNull);
        }
        return String.valueOf(value);
    }

    private String readErrorBody(org.springframework.http.client.ClientHttpResponse clientResponse) {
        try {
            if (clientResponse.getBody() == null) {
                return "";
            }
            String responseBody = new String(clientResponse.getBody().readAllBytes());
            return responseBody.isBlank() ? "" : " body=" + responseBody;
        } catch (Exception ignored) {
            return "";
        }
    }
}
