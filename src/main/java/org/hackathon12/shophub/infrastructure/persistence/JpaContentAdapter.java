package org.hackathon12.shophub.infrastructure.persistence;

import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.port.ContentPort;
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
    public ContentItem findById(UUID contentId) {
        return contentItemJpaRepository.findById(contentId)
                .map(ContentItemEntity::toDomain)
                .orElse(null);
    }

    @Override
    public ContentItem save(ContentItem contentItem) {
        ContentItemEntity saved = contentItemJpaRepository.save(ContentItemEntity.fromDomain(contentItem));
        return saved.toDomain();
    }
}
