package org.hackathon12.shophub.domain.dashboard.service;

import org.hackathon12.shophub.domain.content.model.ContentChannel;
import org.hackathon12.shophub.domain.content.model.ContentItem;
import org.hackathon12.shophub.domain.content.model.ContentStatus;
import org.hackathon12.shophub.domain.content.service.ContentService;
import org.hackathon12.shophub.domain.dashboard.model.DashboardOverview;
import org.hackathon12.shophub.domain.review.model.StoreReview;
import org.hackathon12.shophub.domain.review.model.StoreReviewSummary;
import org.hackathon12.shophub.domain.review.service.StoreReviewService;
import org.hackathon12.shophub.domain.store.model.BusinessHour;
import org.hackathon12.shophub.domain.store.model.StoreProfile;
import org.hackathon12.shophub.domain.store.service.StoreProfileService;
import org.hackathon12.shophub.domain.weather.model.WeatherSnapshot;
import org.hackathon12.shophub.infrastructure.weather.openmeteo.OpenMeteoWeatherClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private static final int MAX_DRAFT_CHAR_COUNT = 2200;
    private static final List<String> DEFAULT_CHANNELS = List.of(
            ContentChannel.INSTAGRAM.name(),
            ContentChannel.NAVER_BLOG.name(),
            ContentChannel.FACEBOOK.name()
    );

    private final StoreProfileService storeProfileService;
    private final ContentService contentService;
    private final StoreReviewService storeReviewService;
    private final OpenMeteoWeatherClient weatherClient;
    private final WeatherOperationSuggestionService weatherOperationSuggestionService;

    public DashboardService(
            StoreProfileService storeProfileService,
            ContentService contentService,
            StoreReviewService storeReviewService,
            OpenMeteoWeatherClient weatherClient,
            WeatherOperationSuggestionService weatherOperationSuggestionService
    ) {
        this.storeProfileService = storeProfileService;
        this.contentService = contentService;
        this.storeReviewService = storeReviewService;
        this.weatherClient = weatherClient;
        this.weatherOperationSuggestionService = weatherOperationSuggestionService;
    }

    public DashboardOverview getOverview(UUID storeId) {
        StoreProfile store = storeProfileService.getStore(storeId);
        List<ContentItem> contents = contentService.getContents(storeId, null);
        List<StoreReview> reviews = storeReviewService.getReviews(storeId, null);

        ContentItem latestDraft = contents.stream()
                .filter(item -> item.status() == ContentStatus.DRAFT)
                .max(Comparator.comparing(ContentItem::updatedAt))
                .orElse(null);

        DashboardOverview.DraftContent draftContent = latestDraft == null
                ? new DashboardOverview.DraftContent("", "", 0, MAX_DRAFT_CHAR_COUNT, DEFAULT_CHANNELS)
                : new DashboardOverview.DraftContent(
                        latestDraft.title(),
                        latestDraft.body(),
                        latestDraft.body().length(),
                        MAX_DRAFT_CHAR_COUNT,
                        latestDraft.channels()
                );

        StoreReviewSummary summary = storeReviewService.getSummary(storeId);
        List<DashboardOverview.RecentReviewItem> recent = reviews.stream()
                .limit(2)
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
                draftContent,
                buildSuggestionCard(store, latestDraft),
                new DashboardOverview.ReviewWidget(
                        summary.externalTotalReviewCount(),
                        summary.localSyncedReviewCount(),
                        "/v1/stores/" + storeId + "/reviews"
                ),
                buildChecklist(store, contents, reviews),
                recent,
                Instant.now()
        );
    }

    private DashboardOverview.SuggestionCard buildSuggestionCard(StoreProfile store, ContentItem latestDraft) {
        WeatherSnapshot weather = weatherClient.lookupByAddress(store.address()).orElse(null);
        return weatherOperationSuggestionService.buildSuggestionCard(weather, latestDraft != null);
    }

    private List<DashboardOverview.ChecklistItem> buildChecklist(
            StoreProfile store,
            List<ContentItem> contents,
            List<StoreReview> reviews
    ) {
        boolean hoursConfigured = isTodayOpen(store.businessHours());
        boolean replyReviewCompleted = reviews.stream()
                .noneMatch(review -> !StringUtils.hasText(review.reply()));
        boolean publishContentCompleted = contents.stream()
                .anyMatch(content -> content.status() == ContentStatus.PUBLISHED
                        && Duration.between(content.updatedAt(), Instant.now()).toHours() < 24);

        return List.of(
                new DashboardOverview.ChecklistItem("check-hours", "오늘 영업시간 확인", hoursConfigured),
                new DashboardOverview.ChecklistItem("reply-review", "최근 MockMap 리뷰에 답글 달기", replyReviewCompleted),
                new DashboardOverview.ChecklistItem("publish-content", "오늘의 게시물 발행하기", publishContentCompleted)
        );
    }

    private boolean isTodayOpen(List<BusinessHour> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) {
            return false;
        }

        String today = toDayCode(DayOfWeek.from(java.time.LocalDate.now()));
        return businessHours.stream()
                .anyMatch(hour -> today.equals(hour.dayOfWeek()) && hour.open());
    }

    private String toDayCode(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
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
