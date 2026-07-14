package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String uploadDir,
        String publicBaseUrl,
        String publicImageProvider,
        S3 s3
) {

    public record S3(
            String bucket,
            String region,
            String publicBaseUrl,
            String keyPrefix,
            String accessKeyId,
            String secretAccessKey
    ) {
    }
}
