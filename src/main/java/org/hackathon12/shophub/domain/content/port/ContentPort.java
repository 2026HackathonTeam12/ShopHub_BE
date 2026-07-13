package org.hackathon12.shophub.domain.content.port;

import org.hackathon12.shophub.domain.content.model.ContentItem;

import java.util.List;
import java.util.UUID;

public interface ContentPort {

    List<ContentItem> findByStoreId(UUID storeId);

    ContentItem findById(UUID contentId);

    ContentItem save(ContentItem contentItem);
}
