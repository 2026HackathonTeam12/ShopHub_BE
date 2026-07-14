package org.hackathon12.shophub.domain.review.model;

public enum ReviewPlatform {
    MOCK_MAP,
    KAKAO_MAP,
    GOOGLE_MAP,
    NAVER_MAP;

    public static ReviewPlatform fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("platform 값은 필수입니다.");
        }

        String normalized = value.trim().toUpperCase();
        try {
            return ReviewPlatform.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "지원하지 않는 리뷰 플랫폼입니다: " + value
                            + " (허용: MOCK_MAP, KAKAO_MAP, GOOGLE_MAP, NAVER_MAP)"
            );
        }
    }
}
