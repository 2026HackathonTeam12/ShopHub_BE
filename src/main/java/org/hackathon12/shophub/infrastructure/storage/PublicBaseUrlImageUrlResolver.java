package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "public-image-provider", havingValue = "public-base-url")
public class PublicBaseUrlImageUrlResolver implements PublicImageUrlResolver {

    private final StorageProperties storageProperties;

    public PublicBaseUrlImageUrlResolver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String resolve(MultipartFile image, String savedFileName, int index) {
        String baseUrl = storageProperties.publicBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("PUBLIC_BASE_URL이 설정되지 않았습니다.");
        }
        return trimTrailingSlash(baseUrl) + "/uploads/instagram/" + savedFileName;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
