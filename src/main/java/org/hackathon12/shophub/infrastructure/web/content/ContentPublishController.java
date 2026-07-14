package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.facebook.service.FacebookPublishService;
import org.hackathon12.shophub.domain.instagram.model.InstagramPublishResult;
import org.hackathon12.shophub.domain.instagram.service.InstagramPublishService;
import org.hackathon12.shophub.domain.x.service.XPublishService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/contents/{type}")
public class ContentPublishController {

    private final InstagramPublishService instagramPublishService;
    private final XPublishService xPublishService;
    private final FacebookPublishService facebookPublishService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public ContentPublishController(
            InstagramPublishService instagramPublishService,
            XPublishService xPublishService,
            FacebookPublishService facebookPublishService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.instagramPublishService = instagramPublishService;
        this.xPublishService = xPublishService;
        this.facebookPublishService = facebookPublishService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @PostMapping("/publish-carousel")
    public InstagramPublishResult publishCarousel(
            @PathVariable UUID storeId,
            @PathVariable String type,
            @RequestBody PublishContentCarouselRequest requestBody,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        ContentChannel channel = ContentChannel.fromValue(type);
        List<String> imageUrls = normalizeImageUrls(requestBody.imageUrls(), channel);

        return switch (channel) {
            case INSTAGRAM -> {
                instagramPublishService.ensurePublishReady();
                yield instagramPublishService.generateAndPublish(storeId, imageUrls);
            }
            case X -> {
                xPublishService.ensurePublishReady(storeId);
                yield xPublishService.generateAndPublish(storeId, imageUrls);
            }
            case FACEBOOK -> {
                facebookPublishService.ensurePublishReady();
                yield facebookPublishService.generateAndPublish(storeId, imageUrls);
            }
            case NAVER_BLOG -> throw new IllegalArgumentException(
                    "아직 지원하지 않는 콘텐츠 채널입니다: " + channel.name()
            );
        };
    }

    private List<String> normalizeImageUrls(List<String> imageUrls, ContentChannel channel) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("imageUrls는 최소 1개 이상 필요합니다.");
        }
        int maxImages = channel == ContentChannel.X ? 4 : 10;
        if (imageUrls.size() > maxImages) {
            throw new IllegalArgumentException(
                    channel.name() + " 게시 이미지는 최대 " + maxImages + "장까지 지원합니다."
            );
        }

        return imageUrls.stream()
                .map(this::normalizeImageUrl)
                .toList();
    }

    private String normalizeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalArgumentException("imageUrls 항목은 비어 있을 수 없습니다.");
        }

        String trimmed = imageUrl.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다: " + imageUrl);
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("이미지 URL은 http 또는 https여야 합니다: " + imageUrl);
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다: " + imageUrl);
        }

        return trimmed;
    }

    public record PublishContentCarouselRequest(
            List<String> imageUrls
    ) {
    }
}
