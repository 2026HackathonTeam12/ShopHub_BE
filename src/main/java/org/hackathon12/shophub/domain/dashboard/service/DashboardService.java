package org.hackathon12.shophub.domain.dashboard.service;

import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.service.ContentService;
import org.hackathon12.shophub.domain.dashboard.model.DashboardOverview;
import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.model.StoreReviewSummary;
import org.hackathon12.shophub.domain.review.service.StoreReviewService;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final StoreProfileService storeProfileService;
    private final ContentService contentService;
    private final StoreReviewService storeReviewService;

    public DashboardService(
            StoreProfileService storeProfileService,
            ContentService contentService,
            StoreReviewService storeReviewService
    ) {
        this.storeProfileService = storeProfileService;
        this.contentService = contentService;
        this.storeReviewService = storeReviewService;
    }

    public DashboardOverview getOverview(UUID storeId) {
        StoreProfile store = storeProfileService.getStore(storeId);
        ContentItem draft = contentService.getContents(storeId, ContentStatus.DRAFT).stream()
                .findFirst()
                .orElse(new ContentItem(
                        UUID.randomUUID(),
                        storeId,
                        "오늘의 추천 메뉴",
                        "비 오는 날엔 따뜻한 커피와 디저트를 함께 소개해보세요.",
                        List.of("Instagram", "네이버 플레이스", "Google Business"),
                        ContentStatus.DRAFT,
                        Instant.now()
                ));

        StoreReviewSummary summary = storeReviewService.getSummary(storeId);
        List<StoreReview> reviews = storeReviewService.getReviews(storeId, null).stream()
                .limit(2)
                .toList();

        List<DashboardOverview.RecentReviewItem> recent = reviews.stream()
                .map(review -> new DashboardOverview.RecentReviewItem(
                        review.id(),
                        review.authorName(),
                        review.rating(),
                        review.content(),
                        toAgeLabel(review.reviewedAt())
                ))
                .toList();

        return new DashboardOverview(
                storeId,
                store.name(),
                new DashboardOverview.DraftContent(
                        draft.title(),
                        draft.body(),
                        draft.body().length(),
                        2200,
                        draft.channels()
                ),
                new DashboardOverview.SuggestionCard(
                        "오늘의 운영 제안",
                        "비 오는 오후, 따뜻한 한 잔 콘텐츠를 지금 올려보세요.",
                        "이 주제로 콘텐츠 만들기"
                ),
                new DashboardOverview.ReviewWidget(
                        summary.totalReviewCount(),
                        summary.syncedReviewCount(),
                        "/v1/stores/" + storeId + "/reviews"
                ),
                List.of(
                        new DashboardOverview.ChecklistItem("check-hours", "오늘 영업시간 확인", true),
                        new DashboardOverview.ChecklistItem("reply-review", "최근 Google 리뷰에 답글 달기", false),
                        new DashboardOverview.ChecklistItem("publish-content", "오늘의 게시물 발행하기", false)
                ),
                recent,
                Instant.now()
        );
    }

    private String toAgeLabel(Instant reviewedAt) {
        long minutes = Math.max(1, Duration.between(reviewedAt, Instant.now()).toMinutes());
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }
        return (hours / 24) + "일 전";
    }
}
