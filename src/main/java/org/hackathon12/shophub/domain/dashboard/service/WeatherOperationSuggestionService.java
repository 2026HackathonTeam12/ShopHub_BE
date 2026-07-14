package org.hackathon12.shophub.domain.dashboard.service;

import org.hackathon12.shophub.domain.dashboard.model.DashboardOverview;
import org.hackathon12.shophub.domain.weather.model.WeatherSnapshot;
import org.springframework.stereotype.Service;

@Service
public class WeatherOperationSuggestionService {

    public DashboardOverview.SuggestionCard buildSuggestionCard(
            WeatherSnapshot weather,
            boolean hasDraft
    ) {
        String title = weather == null ? "날씨 정보 없음" : weather.summaryLabel();
        String weatherMessage = buildWeatherMessage(weather);
        String message = hasDraft
                ? weatherMessage + " 작성 중인 초안을 검토하고 게시해 보세요."
                : weatherMessage;
        String actionLabel = hasDraft ? "초안 이어서 작성하기" : "이 주제로 콘텐츠 만들기";
        return new DashboardOverview.SuggestionCard(title, message, actionLabel);
    }

    private String buildWeatherMessage(WeatherSnapshot weather) {
        if (weather == null) {
            return "오늘 날씨에 맞는 메뉴나 이벤트로 콘텐츠를 시작해 보세요.";
        }

        return switch (weather.weatherCode()) {
            case 61, 63, 65, 66, 67, 80, 81, 82, 51, 53, 55, 56, 57 ->
                    "비 오는 날엔 따뜻한 음료·디저트를 강조해 실내 방문을 유도해 보세요.";
            case 71, 73, 75, 77, 85, 86 ->
                    "눈 오는 날엔 포근한 분위기와 핫메뉴를 홍보하면 좋아요.";
            case 95, 96, 99 ->
                    "폭우·뇌우에는 안전한 실내 이용과 배달·포장 혜택을 알려 보세요.";
            case 0, 1, 2 ->
                    "맑은 날엔 시원한 음료나 야외 좌석, 산책 후 방문 포인트를 강조해 보세요.";
            default -> {
                if (weather.temperatureCelsius() >= 28) {
                    yield "더운 날엔 아이스 음료·냉메뉴를 전면에 내세워 보세요.";
                }
                if (weather.temperatureCelsius() <= 5) {
                    yield "추운 날엔 따뜻한 음료와 실내 휴식 포인트를 강조해 보세요.";
                }
                yield "오늘 날씨에 맞는 시즌 메뉴나 이벤트로 콘텐츠를 시작해 보세요.";
            }
        };
    }
}
