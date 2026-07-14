package org.hackathon12.shophub.domain.dashboard.service;

import org.hackathon12.shophub.domain.dashboard.model.DashboardOverview;
import org.hackathon12.shophub.domain.weather.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherOperationSuggestionServiceTest {

    private final WeatherOperationSuggestionService service = new WeatherOperationSuggestionService();

    @Test
    void rainy_weather_suggests_indoor_menu() {
        DashboardOverview.SuggestionCard card = service.buildSuggestionCard(
                new WeatherSnapshot("비", 18, 61),
                false
        );

        assertEquals("비 · 18°C", card.title());
        assertTrue(card.message().contains("따뜻한 음료"));
        assertEquals("이 주제로 콘텐츠 만들기", card.actionLabel());
    }

    @Test
    void draft_exists_appends_review_message() {
        DashboardOverview.SuggestionCard card = service.buildSuggestionCard(
                new WeatherSnapshot("맑음", 24, 0),
                true
        );

        assertEquals("맑음 · 24°C", card.title());
        assertTrue(card.message().contains("초안"));
        assertEquals("초안 이어서 작성하기", card.actionLabel());
    }

    @Test
    void hot_weather_suggests_iced_menu() {
        DashboardOverview.SuggestionCard card = service.buildSuggestionCard(
                new WeatherSnapshot("구름", 30, 3),
                false
        );

        assertTrue(card.message().contains("아이스"));
    }
}
