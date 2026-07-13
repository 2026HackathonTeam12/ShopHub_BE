package org.hackathon12.shophub.domain.instagram.port;

import java.util.List;

public interface InstagramPublishPort {

    String publishPost(String caption, List<String> imageUrls);
}
