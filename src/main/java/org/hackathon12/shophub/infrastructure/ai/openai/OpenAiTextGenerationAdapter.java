package org.hackathon12.shophub.infrastructure.ai.openai;

import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.model.InstagramCaptionPrompt;
import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;
import org.hackathon12.shophub.domain.ai.port.AiTextGenerationPort;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiTextGenerationAdapter implements AiTextGenerationPort {

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiTextGenerationAdapter(RestClient.Builder restClientBuilder, OpenAiProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public ContentSuggestion generateContentSuggestion(ContentSuggestionPrompt prompt) {
        ContentSuggestion fallback = fallbackContentSuggestion(prompt);
        if (!StringUtils.hasText(properties.apiKey())) {
            return fallback;
        }

        String systemPrompt = """
                당신은 소상공인 콘텐츠 매니저입니다.
                반드시 아래 JSON 형식으로만 응답하세요.
                {"title":"...","body":"..."}
                """;
        String userPrompt = """
                아래 정보를 참고해 게시물 제목과 본문을 작성하세요.
                - 가게명: %s
                - 업종: %s
                - 매장 말투: %s
                - 운영시간: %s
                - 대표 메뉴: %s
                - 이벤트/공지: %s

                조건:
                1) title은 12~30자
                2) body는 2~4문장
                3) 과장광고 금지
                4) 방문 유도 문장 1개 포함
                """.formatted(
                safe(prompt.storeName()),
                safe(prompt.category()),
                safe(prompt.toneOfVoice()),
                safe(prompt.businessHoursSummary()),
                safe(prompt.menuSummary()),
                safe(prompt.eventText())
        );

        String rawResponse = askOpenAi(systemPrompt, userPrompt, null);
        if (!StringUtils.hasText(rawResponse)) {
            return fallback;
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String title = root.path("title").asText();
            String body = root.path("body").asText();
            if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) {
                return fallback;
            }
            return new ContentSuggestion(title.trim(), body.trim());
        } catch (Exception exception) {
            return fallback;
        }
    }

    @Override
    public String generateInstagramCaption(InstagramCaptionPrompt prompt) {
        String fallback = fallbackInstagramCaption(prompt);
        if (!StringUtils.hasText(properties.apiKey())) {
            return fallback;
        }

        String systemPrompt = """
                당신은 소상공인 매장의 인스타그램 카피라이터입니다.
                출력은 한국어 캡션 본문 텍스트만 작성하고, 코드블록/설명/불릿은 금지합니다.
                말투는 정중하고 따뜻하게 유지합니다.
                """;
        String userPrompt = """
                아래 매장 정보를 참고해 인스타그램 캡션을 1개 생성하세요.
                - 매장명: %s
                - 카테고리: %s
                - 매장 말투: %s
                - 영업시간 요약: %s
                - 대표 메뉴: %s
                - 게시 주제: %s
                - 게시 초안 본문: %s

                조건:
                1) 본문은 2~4문장
                2) 과장 표현 금지
                3) 방문 유도 문장 1개 포함
                """.formatted(
                safe(prompt.storeName()),
                safe(prompt.category()),
                safe(prompt.toneOfVoice()),
                safe(prompt.businessHoursSummary()),
                safe(prompt.menuSummary()),
                safe(prompt.contentTitle()),
                safe(prompt.contentBody())
        );

        return askOpenAi(systemPrompt, userPrompt, fallback);
    }

    @Override
    public String generateReviewReply(ReviewReplyPrompt prompt) {
        String fallback = fallbackReviewReply(prompt);
        if (!StringUtils.hasText(properties.apiKey())) {
            return fallback;
        }

        String systemPrompt = """
                당신은 매장 리뷰 답글 작성 비서입니다.
                출력은 한국어 답글 본문 텍스트만 작성하고, 코드블록/설명/불릿은 금지합니다.
                답글 톤은 항상 정중하고 따뜻하게 유지합니다.
                """;
        String userPrompt = """
                아래 리뷰에 대한 답글을 1개 생성하세요.
                - 매장명: %s
                - 평점: %d
                - 리뷰 내용: %s

                조건:
                1) 2~3문장
                2) 과도한 마케팅 문구 금지
                3) 평점이 낮으면 사과와 개선 의지 포함
                4) 평점이 높으면 감사 인사 포함
                """.formatted(
                safe(prompt.storeName()),
                prompt.rating(),
                safe(prompt.reviewContent())
        );

        return askOpenAi(systemPrompt, userPrompt, fallback);
    }

    private String askOpenAi(String systemPrompt, String userPrompt, String fallback) {
        try {
            OpenAiChatCompletionResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(Map.of(
                            "model", safeModel(),
                            "temperature", safeTemperature(),
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .body(OpenAiChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return fallback == null ? "" : fallback;
            }
            String content = response.choices().get(0).message() == null
                    ? null
                    : response.choices().get(0).message().content();
            return StringUtils.hasText(content) ? content.trim() : (fallback == null ? "" : fallback);
        } catch (Exception exception) {
            return fallback == null ? "" : fallback;
        }
    }

    private String safeModel() {
        return StringUtils.hasText(properties.model()) ? properties.model() : "gpt-4o-mini";
    }

    private double safeTemperature() {
        return properties.temperature() == null ? 0.7 : properties.temperature();
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String fallbackInstagramCaption(InstagramCaptionPrompt prompt) {
        return "%s의 오늘 추천 소식을 전해드립니다. %s %s\n운영시간은 %s이며, 편하게 들러주세요."
                .formatted(
                        safe(prompt.storeName()),
                        safe(prompt.contentTitle()),
                        safe(prompt.contentBody()),
                        safe(prompt.businessHoursSummary())
                );
    }

    private ContentSuggestion fallbackContentSuggestion(ContentSuggestionPrompt prompt) {
        return new ContentSuggestion(
                safe(prompt.storeName()) + " 오늘의 소식",
                "%s 관련 안내드립니다. %s\n운영시간은 %s이며, 대표 메뉴는 %s입니다."
                        .formatted(
                                safe(prompt.category()),
                                safe(prompt.eventText()),
                                safe(prompt.businessHoursSummary()),
                                safe(prompt.menuSummary())
                        )
        );
    }

    private String fallbackReviewReply(ReviewReplyPrompt prompt) {
        if (prompt.rating() <= 2) {
            return "소중한 의견 남겨주셔서 감사합니다. 불편을 드려 죄송하며 말씀해주신 부분을 빠르게 점검하고 개선하겠습니다.";
        }
        if (prompt.rating() == 3) {
            return "리뷰 남겨주셔서 감사합니다. 남겨주신 의견을 반영해 더 만족스러운 방문 경험을 드릴 수 있도록 노력하겠습니다.";
        }
        return "따뜻한 리뷰 감사합니다. 다음 방문에서도 편안하고 좋은 경험을 드릴 수 있도록 정성껏 준비하겠습니다.";
    }

    private record OpenAiChatCompletionResponse(
            List<OpenAiChoice> choices
    ) {
    }

    private record OpenAiChoice(
            OpenAiMessage message
    ) {
    }

    private record OpenAiMessage(
            String content
    ) {
    }
}
