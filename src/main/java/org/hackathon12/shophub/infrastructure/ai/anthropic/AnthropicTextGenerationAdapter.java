package org.hackathon12.shophub.infrastructure.ai.anthropic;

import org.hackathon12.shophub.domain.ai.model.AiGeneratedText;
import org.hackathon12.shophub.domain.ai.model.AiGenerationSource;
import org.hackathon12.shophub.domain.ai.model.ContentSuggestionPrompt;
import org.hackathon12.shophub.domain.ai.model.InstagramCaptionPrompt;
import org.hackathon12.shophub.domain.ai.model.ReviewReplyPrompt;
import org.hackathon12.shophub.domain.ai.port.AiTextGenerationPort;
import org.hackathon12.shophub.domain.content.model.ContentSuggestion;
import org.hackathon12.shophub.infrastructure.ai.template.AiTextTemplateProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@Primary
@ConditionalOnProperty(prefix = "anthropic", name = "api-key")
public class AnthropicTextGenerationAdapter implements AiTextGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(AnthropicTextGenerationAdapter.class);

    private final RestClient restClient;
    private final AnthropicProperties properties;
    private final AiTextTemplateProvider templateProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicTextGenerationAdapter(
            RestClient.Builder restClientBuilder,
            AnthropicProperties properties,
            AiTextTemplateProvider templateProvider
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.templateProvider = templateProvider;
    }

    @Override
    public ContentSuggestion generateContentSuggestion(ContentSuggestionPrompt prompt) {
        AiTextTemplateProvider.TemplateContent template = templateProvider.contentSuggestion(prompt);

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

        String rawResponse = callAnthropic(systemPrompt, userPrompt);
        if (!StringUtils.hasText(rawResponse)) {
            log.warn("Anthropic content suggestion fallback to template: empty response");
            return toContentSuggestion(template, AiGenerationSource.TEMPLATE);
        }

        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(rawResponse));
            String title = root.path("title").asText();
            String body = root.path("body").asText();
            if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) {
                log.warn("Anthropic content suggestion fallback to template: invalid JSON fields");
                return toContentSuggestion(template, AiGenerationSource.TEMPLATE);
            }
            return new ContentSuggestion(title.trim(), body.trim(), AiGenerationSource.AI);
        } catch (Exception exception) {
            log.warn("Anthropic content suggestion fallback to template: {}", exception.getMessage());
            return toContentSuggestion(template, AiGenerationSource.TEMPLATE);
        }
    }

    @Override
    public AiGeneratedText generateInstagramCaption(InstagramCaptionPrompt prompt) {
        String template = templateProvider.instagramCaption(prompt);

        String systemPrompt = """
                당신은 소상공인 매장의 SNS 카피라이터입니다.
                출력은 한국어 본문 텍스트만 작성하고, 코드블록/설명/불릿은 금지합니다.
                말투는 정중하고 따뜻하게 유지합니다.
                """;
        String userPrompt = """
                아래 매장 정보를 참고해 SNS 게시 문구를 1개 생성하세요.
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

        String aiResult = callAnthropic(systemPrompt, userPrompt);
        if (!StringUtils.hasText(aiResult)) {
            return new AiGeneratedText(template, AiGenerationSource.TEMPLATE);
        }
        return new AiGeneratedText(aiResult.trim(), AiGenerationSource.AI);
    }

    @Override
    public AiGeneratedText generateReviewReply(ReviewReplyPrompt prompt) {
        String template = templateProvider.reviewReply(prompt);

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

        String aiResult = callAnthropic(systemPrompt, userPrompt);
        if (!StringUtils.hasText(aiResult)) {
            return new AiGeneratedText(template, AiGenerationSource.TEMPLATE);
        }
        return new AiGeneratedText(aiResult.trim(), AiGenerationSource.AI);
    }

    private String callAnthropic(String systemPrompt, String userPrompt) {
        try {
            AnthropicMessageResponse response = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", safeApiVersion())
                    .body(Map.of(
                            "model", safeModel(),
                            "max_tokens", safeMaxTokens(),
                            "system", systemPrompt,
                            "messages", List.of(
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .body(AnthropicMessageResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                log.warn("Anthropic API returned empty content (model={})", safeModel());
                return null;
            }

            String text = response.content().get(0).text();
            return StringUtils.hasText(text) ? text : null;
        } catch (Exception exception) {
            log.warn("Anthropic API call failed (model={}): {}", safeModel(), exception.getMessage());
            return null;
        }
    }

    private String extractJsonPayload(String rawResponse) {
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int closingFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && closingFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, closingFence).trim();
            }
        }
        return trimmed;
    }

    private record AnthropicMessageResponse(
            List<AnthropicContentBlock> content
    ) {
    }

    private record AnthropicContentBlock(
            String type,
            String text
    ) {
    }

    private ContentSuggestion toContentSuggestion(
            AiTextTemplateProvider.TemplateContent template,
            AiGenerationSource source
    ) {
        return new ContentSuggestion(template.title(), template.body(), source);
    }

    private String safeModel() {
        return StringUtils.hasText(properties.model()) ? properties.model() : "claude-sonnet-4-6";
    }

    private int safeMaxTokens() {
        return properties.maxTokens() == null ? 1024 : properties.maxTokens();
    }

    private String safeApiVersion() {
        return StringUtils.hasText(properties.apiVersion()) ? properties.apiVersion() : "2023-06-01";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }
}
