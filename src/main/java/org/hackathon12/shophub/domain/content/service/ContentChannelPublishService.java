package org.hackathon12.shophub.domain.content.service;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.port.ContentPort;
import org.hackathon12.shophub.domain.facebook.service.FacebookPublishService;
import org.hackathon12.shophub.domain.instagram.service.InstagramPublishService;
import org.hackathon12.shophub.domain.x.service.XPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ContentChannelPublishService {

    private static final Logger log = LoggerFactory.getLogger(ContentChannelPublishService.class);

    private final ContentPort contentPort;
    private final ContentImageUrlNormalizer contentImageUrlNormalizer;
    private final InstagramPublishService instagramPublishService;
    private final XPublishService xPublishService;
    private final FacebookPublishService facebookPublishService;

    public ContentChannelPublishService(
            ContentPort contentPort,
            ContentImageUrlNormalizer contentImageUrlNormalizer,
            InstagramPublishService instagramPublishService,
            XPublishService xPublishService,
            FacebookPublishService facebookPublishService
    ) {
        this.contentPort = contentPort;
        this.contentImageUrlNormalizer = contentImageUrlNormalizer;
        this.instagramPublishService = instagramPublishService;
        this.xPublishService = xPublishService;
        this.facebookPublishService = facebookPublishService;
    }

    public ContentItem publishToChannels(UUID storeId, ContentItem content, List<String> imgUrls) {
        boolean anySuccess = false;

        for (String channelName : content.channels()) {
            ContentChannel channel = ContentChannel.fromValue(channelName);

            try {
                List<String> normalizedImgUrls = resolveImageUrls(imgUrls, channel);
                publishToChannel(storeId, channel, content, normalizedImgUrls);
                content = contentPort.updatePlatformStatus(content.id(), channelName, ContentChannelPublishStatus.SUCCESS);
                anySuccess = true;
            } catch (RuntimeException exception) {
                log.warn(
                        "Content channel publish failed: storeId={}, contentId={}, channel={}, message={}",
                        storeId,
                        content.id(),
                        channelName,
                        exception.getMessage()
                );
                content = contentPort.updatePlatformStatus(content.id(), channelName, ContentChannelPublishStatus.FAILED);
            }
        }

        ContentStatus nextStatus = anySuccess ? ContentStatus.PUBLISHED : ContentStatus.FAILED;
        return contentPort.updateContentStatus(content.id(), nextStatus);
    }

    private List<String> resolveImageUrls(List<String> imgUrls, ContentChannel channel) {
        List<String> urls = imgUrls == null ? List.of() : imgUrls;
        if (urls.isEmpty()) {
            if (channel == ContentChannel.X || channel == ContentChannel.FACEBOOK) {
                return List.of();
            }
            throw new IllegalArgumentException(
                    "Instagram 게시에는 최소 1개 이상의 이미지가 필요합니다."
            );
        }
        return contentImageUrlNormalizer.normalize(urls, channel);
    }

    private void publishToChannel(
            UUID storeId,
            ContentChannel channel,
            ContentItem content,
            List<String> imgUrls
    ) {
        switch (channel) {
            case INSTAGRAM -> {
                instagramPublishService.ensurePublishReady();
                instagramPublishService.publishContent(storeId, content, imgUrls);
            }
            case X -> {
                xPublishService.ensurePublishReady(storeId);
                xPublishService.publishContent(storeId, content, imgUrls);
            }
            case FACEBOOK -> {
                facebookPublishService.ensurePublishReady();
                facebookPublishService.publishContent(storeId, content, imgUrls);
            }
            case NAVER_BLOG -> throw new IllegalArgumentException(
                    "아직 지원하지 않는 콘텐츠 채널입니다: " + channel.name()
            );
        }
    }
}
