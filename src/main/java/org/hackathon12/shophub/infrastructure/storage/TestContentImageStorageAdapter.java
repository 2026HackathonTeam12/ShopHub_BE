package org.hackathon12.shophub.infrastructure.storage;

import org.hackathon12.shophub.domain.content.port.ContentImageStoragePort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@Profile("test")
@Primary
public class TestContentImageStorageAdapter implements ContentImageStoragePort {

    @Override
    public List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 최소 1개 필요합니다.");
        }

        return IntStream.range(0, images.size())
                .mapToObj(index -> "https://test-bucket.s3.ap-northeast-2.amazonaws.com/shophub/content/"
                        + UUID.randomUUID() + ".jpg")
                .toList();
    }
}
