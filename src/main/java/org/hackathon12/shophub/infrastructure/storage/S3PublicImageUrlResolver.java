package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "public-image-provider", havingValue = "s3")
public class S3PublicImageUrlResolver implements PublicImageUrlResolver {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3PublicImageUrlResolver(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String resolve(MultipartFile image, String savedFileName, int index) {
        StorageProperties.S3 s3 = storageProperties.s3();
        if (s3 == null || !StringUtils.hasText(s3.bucket())) {
            throw new IllegalArgumentException("AWS_S3_BUCKET이 설정되지 않았습니다.");
        }

        String objectKey = buildObjectKey(s3, savedFileName);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3.bucket())
                    .key(objectKey)
                    .contentType(image.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(image.getBytes()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("S3 이미지 업로드에 실패했습니다: " + image.getOriginalFilename(), exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("S3 이미지 업로드에 실패했습니다: " + exception.getMessage(), exception);
        }

        return buildPublicUrl(s3, objectKey);
    }

    private String buildObjectKey(StorageProperties.S3 s3, String savedFileName) {
        String prefix = StringUtils.hasText(s3.keyPrefix()) ? trimSlashes(s3.keyPrefix()) : "shophub";
        return prefix + "/instagram/" + savedFileName;
    }

    private String buildPublicUrl(StorageProperties.S3 s3, String objectKey) {
        if (StringUtils.hasText(s3.publicBaseUrl())) {
            return trimTrailingSlash(s3.publicBaseUrl()) + "/" + objectKey;
        }
        return "https://" + s3.bucket() + ".s3." + s3.region() + ".amazonaws.com/" + objectKey;
    }

    private String trimSlashes(String value) {
        String trimmed = value;
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
