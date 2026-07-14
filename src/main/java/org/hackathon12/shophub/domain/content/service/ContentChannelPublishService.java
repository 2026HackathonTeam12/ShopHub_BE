package org.hackathon12.shophub.domain.content.service;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.port.ContentPort;
import org.hackathon12.shophub.domain.facebook.service.FacebookPublishService;
import org.hackathon12.shophub.domain.instagram.service.InstagramPublishService;
import org.hackathon12.shophub.domain.x.service.XPublishService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ContentChannelPublishService {

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
        boolean allSuccess = true;

        for (String channelName : content.channels()) {
            ContentChannel channel = ContentChannel.fromValue(channelName);
            List<String> normalizedImgUrls = contentImageUrlNormalizer.normalize(imgUrls, channel);

            try {
                publishToChannel(storeId, channel, content, normalizedImgUrls);
                content = contentPort.updatePlatformStatus(content.id(), channelName, ContentChannelPublishStatus.SUCCESS);
            } catch (RuntimeException exception) {
                allSuccess = false;
                content = contentPort.updatePlatformStatus(content.id(), channelName, ContentChannelPublishStatus.FAILED);
            }
        }

        ContentStatus nextStatus = allSuccess ? ContentStatus.PUBLISHED : ContentStatus.FAILED;
        return contentPort.updateContentStatus(content.id(), nextStatus);
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
