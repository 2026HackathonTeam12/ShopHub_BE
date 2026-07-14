package org.hackathon12.shophub.domain.content.service;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;

@Component
public class ContentImageUrlNormalizer {

    public List<String> normalize(List<String> imgUrls, ContentChannel channel) {
        if (imgUrls == null || imgUrls.isEmpty()) {
            throw new IllegalArgumentException("img_urls는 최소 1개 이상 필요합니다.");
        }

        int maxImages = channel == ContentChannel.X ? 4 : 10;
        if (imgUrls.size() > maxImages) {
            throw new IllegalArgumentException(
                    channel.name() + " 게시 이미지는 최대 " + maxImages + "장까지 지원합니다."
            );
        }

        return imgUrls.stream()
                .map(this::normalizeSingle)
                .toList();
    }

    private String normalizeSingle(String imgUrl) {
        if (!StringUtils.hasText(imgUrl)) {
            throw new IllegalArgumentException("img_urls 항목은 비어 있을 수 없습니다.");
        }

        String trimmed = imgUrl.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다: " + imgUrl);
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("이미지 URL은 http 또는 https여야 합니다: " + imgUrl);
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다: " + imgUrl);
        }

        return trimmed;
    }
}
