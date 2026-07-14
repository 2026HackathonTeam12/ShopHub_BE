package org.hackathon12.shophub.domain.content.model;

import java.util.List;

public enum ContentChannel {
    INSTAGRAM,
    NAVER_BLOG,
    FACEBOOK;

    public static ContentChannel fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("channels 값은 필수입니다.");
        }

        String normalized = value.trim().toUpperCase();
        try {
            return ContentChannel.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "지원하지 않는 콘텐츠 채널입니다: " + value
                            + " (허용: INSTAGRAM, NAVER_BLOG, FACEBOOK)"
            );
        }
    }

    public static List<String> normalizeAll(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("channels 값은 최소 1개 이상 필요합니다.");
        }
        return channels.stream()
                .map(ContentChannel::fromValue)
                .map(Enum::name)
                .distinct()
                .toList();
    }
}
