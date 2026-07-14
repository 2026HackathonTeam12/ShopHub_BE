package org.hackathon12.shophub.infrastructure.storage;

import org.hackathon12.shophub.domain.content.port.ContentImageStoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@ConditionalOnMissingBean(ContentImageStoragePort.class)
public class UnavailableContentImageStorageAdapter implements ContentImageStoragePort {

    @Override
    public List<String> uploadImages(List<MultipartFile> images) {
        throw new IllegalArgumentException(
                "이미지 업로드는 S3 설정이 필요합니다. app.storage.public-image-provider=s3 와 AWS S3 환경변수를 확인해 주세요."
        );
    }
}
