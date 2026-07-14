package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.content.model.ContentChannelPublishStatus;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentPlatformStatusItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.port.ContentPort;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaContentAdapter implements ContentPort {

    private final ContentItemJpaRepository contentItemJpaRepository;

    public JpaContentAdapter(ContentItemJpaRepository contentItemJpaRepository) {
        this.contentItemJpaRepository = contentItemJpaRepository;
    }

    @Override
    public List<ContentItem> findByStoreId(UUID storeId) {
        return contentItemJpaRepository.findByStore_IdOrderByUpdatedAtDesc(storeId).stream()
                .map(ContentItemEntity::toDomain)
                .toList();
    }

    @Override
    public List<ContentPlatformStatusItem> findPlatformStatusesByStoreId(UUID storeId) {
        return contentItemJpaRepository.findByStore_IdOrderByUpdatedAtDesc(storeId).stream()
                .map(ContentItemEntity::toPlatformStatusDomain)
                .toList();
    }

    @Override
    public ContentItem findById(UUID contentId) {
        return contentItemJpaRepository.findById(contentId)
                .map(ContentItemEntity::toDomain)
                .orElse(null);
    }

    @Override
    public ContentItem save(ContentItem contentItem) {
        ContentItemEntity entity = ContentItemEntity.fromDomain(contentItem);
        contentItemJpaRepository.findById(contentItem.id())
                .ifPresent(existing -> entity.mergePlatformStatuses(existing));

        ContentItemEntity saved = contentItemJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public ContentItem resetPlatformStatusesToPending(UUID contentId) {
        ContentItemEntity entity = contentItemJpaRepository.findById(contentId)
                .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId));

        entity.resetAllPlatformStatusesToPending();
        return contentItemJpaRepository.save(entity).toDomain();
    }

    @Override
    public ContentItem updatePlatformStatus(UUID contentId, String channelName, ContentChannelPublishStatus status) {
        ContentItemEntity entity = contentItemJpaRepository.findById(contentId)
                .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId));

        entity.updateChannelPublishStatus(channelName, status);
        entity.setUpdatedAt(java.time.Instant.now());
        return contentItemJpaRepository.save(entity).toDomain();
    }

    @Override
    public ContentItem updateContentStatus(UUID contentId, ContentStatus status) {
        ContentItemEntity entity = contentItemJpaRepository.findById(contentId)
                .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다. contentId=" + contentId));

        entity.setStatus(status);
        entity.setUpdatedAt(java.time.Instant.now());
        return contentItemJpaRepository.save(entity).toDomain();
    }
}
