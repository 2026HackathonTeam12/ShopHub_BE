package org.hackathon12.shophub.domain.x.port;

import java.util.List;
import java.util.UUID;

public interface XPublishPort {

    void ensurePublishReady(UUID storeId);

    String publishPost(UUID storeId, String text, List<String> imageUrls);
}
