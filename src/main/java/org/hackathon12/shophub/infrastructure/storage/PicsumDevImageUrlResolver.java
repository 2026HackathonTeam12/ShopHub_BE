package org.hackathon12.shophub.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ShopHub_InstarExample/test.js 와 동일한 picsum 공개 URL을 사용합니다.
 * Instagram Graph API가 실제로 접근 가능한 URL인지 검증할 때 로컬 개발용으로 사용합니다.
 */
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "public-image-provider", havingValue = "picsum-dev")
public class PicsumDevImageUrlResolver implements PublicImageUrlResolver {

    private static final List<String> SAMPLE_IMAGE_URLS = List.of(
            "https://picsum.photos/id/1015/1080/1080",
            "https://picsum.photos/id/1016/1080/1080",
            "https://picsum.photos/id/1018/1080/1080",
            "https://picsum.photos/id/1025/1080/1080"
    );

    @Override
    public String resolve(MultipartFile image, String savedFileName, int index) {
        return SAMPLE_IMAGE_URLS.get(index % SAMPLE_IMAGE_URLS.size());
    }
}
