package org.hackathon12.shophub.infrastructure.x;

import org.hackathon12.shophub.domain.x.port.XPublishPort;
import org.hackathon12.shophub.infrastructure.x.oauth.XOwnerOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Component
public class XPublishAdapter implements XPublishPort {

    private static final Logger log = LoggerFactory.getLogger(XPublishAdapter.class);

    private static final int MAX_IMAGES = 4;
    private static final int MAX_TWEET_LENGTH = 280;

    private final XOwnerOAuthService xOwnerOAuthService;
    private final XApiClient xApiClient;

    public XPublishAdapter(
            XOwnerOAuthService xOwnerOAuthService,
            XApiClient xApiClient
    ) {
        this.xOwnerOAuthService = xOwnerOAuthService;
        this.xApiClient = xApiClient;
    }

    @Override
    public void ensurePublishReady(UUID storeId) {
        xOwnerOAuthService.getConnectedAccount(storeId);
    }

    @Override
    public String publishPost(UUID storeId, String text, List<String> imageUrls) {
        ensurePublishReady(storeId);
        validateImages(imageUrls);

        String tweetText = normalizeTweetText(text);
        return withFreshAccessToken(storeId, accessToken -> {
            List<String> mediaIds = uploadImagesOrEmpty(accessToken, imageUrls);
            return xApiClient.publishTweet(accessToken, tweetText, mediaIds);
        });
    }

    private String withFreshAccessToken(UUID storeId, Function<String, String> action) {
        try {
            return action.apply(xOwnerOAuthService.getAccessToken(storeId));
        } catch (XApiException exception) {
            if (!isUnauthorized(exception)) {
                throw exception;
            }
            log.warn("X access token rejected, refreshing and retrying: storeId={}", storeId);
            xOwnerOAuthService.invalidateAccessToken(storeId);
            return action.apply(xOwnerOAuthService.getAccessToken(storeId));
        }
    }

    private List<String> uploadImagesOrEmpty(String accessToken, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }

        try {
            return uploadImages(accessToken, imageUrls);
        } catch (XApiException exception) {
            if (isUnauthorized(exception)) {
                throw exception;
            }
            // Free/dev tier or missing media.write often blocks upload; text post still works.
            log.warn("X media upload failed, publishing text-only: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<String> uploadImages(String accessToken, List<String> imageUrls) {
        List<String> mediaIds = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            String imageUrl = imageUrls.get(index);
            byte[] imageBytes = xApiClient.downloadImage(imageUrl);
            String fileName = "tweet-image-" + (index + 1) + guessExtension(imageUrl);
            mediaIds.add(xApiClient.uploadImage(accessToken, imageBytes, fileName));
        }
        return mediaIds;
    }

    private boolean isUnauthorized(XApiException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return message.contains("HTTP 401")
                || message.contains("Invalid or expired token")
                || message.contains("Could not authenticate you");
    }

    private String normalizeTweetText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("X 게시 텍스트는 필수입니다.");
        }
        String trimmed = text.trim();
        if (trimmed.length() <= MAX_TWEET_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_TWEET_LENGTH - 3) + "...";
    }

    private void validateImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        if (imageUrls.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("X 게시 이미지는 최대 4장까지 지원합니다.");
        }
    }

    private String guessExtension(String imageUrl) {
        int queryIndex = imageUrl.indexOf('?');
        String path = queryIndex >= 0 ? imageUrl.substring(0, queryIndex) : imageUrl;
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == path.length() - 1) {
            return ".jpg";
        }
        String extension = path.substring(dotIndex);
        return extension.length() <= 5 ? extension : ".jpg";
    }
}
