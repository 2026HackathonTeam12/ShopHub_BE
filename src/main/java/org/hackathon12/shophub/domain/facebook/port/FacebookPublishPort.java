package org.hackathon12.shophub.domain.facebook.port;

import java.util.List;

public interface FacebookPublishPort {

    void ensurePublishReady();

    String publishPost(String message, List<String> imageUrls);
}
