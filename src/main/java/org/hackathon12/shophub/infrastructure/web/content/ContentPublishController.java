package org.hackathon12.shophub.infrastructure.web.content;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.instagram.model.InstagramPublishResult;
import org.hackathon12.shophub.domain.content.service.ContentImageUrlNormalizer;
import org.hackathon12.shophub.domain.facebook.service.FacebookPublishService;
import org.hackathon12.shophub.domain.instagram.service.InstagramPublishService;
import org.hackathon12.shophub.domain.x.service.XPublishService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores/{storeId}/contents/{type}")
public class ContentPublishController {

    private final InstagramPublishService instagramPublishService;
    private final XPublishService xPublishService;
    private final FacebookPublishService facebookPublishService;
    private final ContentImageUrlNormalizer contentImageUrlNormalizer;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public ContentPublishController(
            InstagramPublishService instagramPublishService,
            XPublishService xPublishService,
            FacebookPublishService facebookPublishService,
            ContentImageUrlNormalizer contentImageUrlNormalizer,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.instagramPublishService = instagramPublishService;
        this.xPublishService = xPublishService;
        this.facebookPublishService = facebookPublishService;
        this.contentImageUrlNormalizer = contentImageUrlNormalizer;
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
        List<String> imageUrls = contentImageUrlNormalizer.normalize(requestBody.imageUrls(), channel);

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

    public record PublishContentCarouselRequest(
            List<String> imageUrls
    ) {
    }
}
