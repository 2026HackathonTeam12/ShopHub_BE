package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "public-image-provider", havingValue = "s3")
public class S3StorageConfiguration {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties storageProperties) {
        StorageProperties.S3 s3 = storageProperties.s3();
        if (s3 == null || !StringUtils.hasText(s3.bucket()) || !StringUtils.hasText(s3.region())) {
            throw new IllegalStateException("S3 사용 시 AWS_S3_BUCKET, AWS_S3_REGION 설정이 필요합니다.");
        }

        return S3Client.builder()
                .region(Region.of(s3.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
