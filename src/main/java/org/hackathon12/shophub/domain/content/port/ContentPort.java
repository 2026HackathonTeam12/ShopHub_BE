package org.hackathon12.shophub.domain.content.port;

import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentPlatformStatusItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;

import java.util.List;
import java.util.UUID;

public interface ContentPort {

    List<ContentItem> findByStoreId(UUID storeId);

    List<ContentPlatformStatusItem> findPlatformStatusesByStoreId(UUID storeId);

    ContentItem findById(UUID contentId);

    ContentItem save(ContentItem contentItem);

    ContentItem updatePlatformStatus(UUID contentId, String channelName, ContentChannelPublishStatus status);

    ContentItem updateContentStatus(UUID contentId, ContentStatus status);

    ContentItem resetPlatformStatusesToPending(UUID contentId);
}
