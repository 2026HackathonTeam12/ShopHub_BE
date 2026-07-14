package org.hackathon12.shophub.infrastructure.storage;

import org.hackathon12.shophub.domain.content.port.ContentImageStoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "public-image-provider", havingValue = "s3")
public class S3ContentImageStorageAdapter implements ContentImageStoragePort {

    private final S3PublicImageUrlResolver s3PublicImageUrlResolver;

    public S3ContentImageStorageAdapter(S3PublicImageUrlResolver s3PublicImageUrlResolver) {
        this.s3PublicImageUrlResolver = s3PublicImageUrlResolver;
    }

    @Override
    public List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 최소 1개 필요합니다.");
        }
        if (images.size() > 10) {
            throw new IllegalArgumentException("업로드 이미지는 최대 10장까지 지원합니다.");
        }

        List<String> imgUrls = new ArrayList<>();
        for (int index = 0; index < images.size(); index++) {
            MultipartFile image = images.get(index);
            validateImage(image);
            String savedFileName = UUID.randomUUID() + extractExtension(image.getOriginalFilename());
            imgUrls.add(s3PublicImageUrlResolver.resolve(image, savedFileName, index));
        }
        return imgUrls;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("업로드 이미지가 비어 있습니다.");
        }
        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
