package org.hackathon12.shophub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.time.Instant;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.review-sync.enabled=false",
        "mock-map.api.base-url=http://127.0.0.1:59999",
        "instagram.graph.account-id=",
        "instagram.graph.access-token=",
        "instagram.graph.allowed-account-id=",
        "openai.api-key="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopHubApiIntegrationTest {

    private static final String STORE_ID = "0f7ed494-8e0e-4c5e-b6f4-5294ee3989d1";
    private static final String NOT_FOUND_STORE_ID = "00000000-0000-0000-0000-000000000000";
    private static final String REVIEW_ID = "d2c39a88-6461-4f93-9f7f-f40103f5205c";
    private static final String SECOND_REVIEW_ID = "74867f6f-27f8-47dd-a838-b6fd4daa618f";
    private static final String THIRD_REVIEW_ID = "b9cfef29-b5d9-4fbb-a3f3-c4ccbb1fdcde";
    private static final String NOT_FOUND_REVIEW_ID = "11111111-1111-1111-1111-111111111111";
    private static final String NOT_FOUND_CONTENT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String FOURTH_REVIEW_ID = "66e81006-bf6f-473b-9c0f-7d4f88f56dd3";
    private static final String FIFTH_REVIEW_ID = "2d59f447-f73c-482b-a8f9-17a1af832d7b";
    private static final String FAILED_CONTENT_ID = "c781ac92-ef95-4cdb-ad87-0379a826f576";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private String cachedSeedToken;

    private String seedToken() throws Exception {
        if (cachedSeedToken == null) {
            cachedSeedToken = loginAsSeedUser();
        }
        return cachedSeedToken;
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) throws Exception {
        return builder.header("Authorization", "Bearer " + seedToken());
    }

    private MockMultipartHttpServletRequestBuilder authed(MockMultipartHttpServletRequestBuilder builder) throws Exception {
        return (MockMultipartHttpServletRequestBuilder) builder.header("Authorization", "Bearer " + seedToken());
    }

    private String loginAsSeedUser() throws Exception {
        return login("name@business.kr", "Passw0rd!");
    }

    private String signUpAndLogin(String email) throws Exception {
        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Passw0rd!",
                                "name", "테스트 사용자"
                        ))))
                .andExpect(status().isCreated());
        return login(email, "Passw0rd!");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder authedAs(String token, MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }


    @Test
    void sign_up_success() throws Exception {
        String email = "new-user-" + UUID.randomUUID() + "@business.kr";
        String request = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "Passw0rd!",
                "name", "신규 운영자"
        ));

        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Passw0rd!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void sign_up_duplicate_email_returns_400() throws Exception {
        String email = "dup-user-" + UUID.randomUUID() + "@business.kr";
        String request = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "Passw0rd!",
                "name", "중복 사용자"
        ));

        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void login_success() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "name@business.kr",
                "password", "Passw0rd!"
        );

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("name@business.kr"));
    }

    @Test
    void login_with_invalid_credentials_returns_401() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "name@business.kr",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void dashboard_overview_can_be_loaded() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/dashboard", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(STORE_ID))
                .andExpect(jsonPath("$.draftContent.maxCharCount").value(2200))
                .andExpect(jsonPath("$.checklistItems.length()").value(greaterThan(0)));
    }

    @Test
    void dashboard_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/dashboard", NOT_FOUND_STORE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void draft_contents_can_be_filtered() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/contents", STORE_ID)
                        .param("status", "DRAFT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void contents_can_be_loaded_without_filter() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/contents", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test
    void content_status_can_be_changed() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "테스트 콘텐츠",
                "body", "테스트 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));

        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void content_status_change_with_mismatched_store_returns_404() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "스토어 검증 콘텐츠",
                "body", "스토어 검증 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));

        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", NOT_FOUND_STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_status_change_for_unknown_content_returns_404() throws Exception {
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));
        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, NOT_FOUND_CONTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_can_be_retried() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "재시도 대상 콘텐츠",
                "body", "재시도 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents/{contentId}/retry", STORE_ID, contentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void content_retry_with_mismatched_store_returns_404() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "재시도 스토어 검증 콘텐츠",
                "body", "재시도 스토어 검증 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents/{contentId}/retry", NOT_FOUND_STORE_ID, contentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_retry_for_unknown_content_returns_404() throws Exception {
        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents/{contentId}/retry", STORE_ID, NOT_FOUND_CONTENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void stores_without_auth_returns_401() throws Exception {
        mockMvc.perform(get("/v1/stores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void stores_can_be_loaded_with_auth() throws Exception {
        mockMvc.perform(authed(get("/v1/stores")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test
    void store_can_be_created() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "테스트 신규 매장",
                "phone", "02-1234-5678",
                "introduction", "신규 매장 소개",
                "address", "서울 마포구 테스트 10",
                "category", "카페 · 디저트",
                "toneOfVoice", "따뜻하고 담백한",
                "menuItems", List.of(
                        Map.of("name", "시그니처 라떼", "description", "대표 메뉴")
                )
        ));

        mockMvc.perform(authed(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("테스트 신규 매장"))
                .andExpect(jsonPath("$.menuItems.length()").value(1));
    }

    @Test
    void store_can_be_created_with_frontend_compatible_payload() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "프론트 호환 매장",
                "phone", "02-2222-3333",
                "description", "프론트 온보딩 설명",
                "neighborhood", "서울 성동구 성수동",
                "address", "서울 성동구 연무장길 31",
                "category", "카페 · 디저트",
                "tone", "따뜻하고 담백한 동네 카페의 말투",
                "hours", "매일 11:00 - 21:00",
                "menu", List.of("오트 크림 라떼", "레몬 파운드")
        ));

        mockMvc.perform(authed(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("프론트 호환 매장"))
                .andExpect(jsonPath("$.toneOfVoice").value("따뜻하고 담백한 동네 카페의 말투"))
                .andExpect(jsonPath("$.menuItems.length()").value(2))
                .andExpect(jsonPath("$.businessHours.length()").value(7));
    }

    @Test
    void store_create_with_missing_required_fields_returns_400() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "필수값누락매장",
                "category", "카페 · 디저트"
        ));

        mockMvc.perform(authed(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void store_profile_can_be_loaded() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/profile", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.menuItems.length()").value(greaterThan(0)));
    }

    @Test
    void store_profile_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/profile", NOT_FOUND_STORE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_hours_update_for_unknown_store_returns_404() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "businessHours", List.of(
                        Map.of("dayOfWeek", "MON", "openTime", "10:00", "closeTime", "20:00", "open", true)
                )
        ));

        mockMvc.perform(authed(put("/v1/stores/{storeId}/profile/hours", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_add_menu_for_unknown_store_returns_404() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "없는 매장 메뉴",
                "description", "설명"
        ));

        mockMvc.perform(authed(post("/v1/stores/{storeId}/profile/menus", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_remove_menu_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(authed(delete("/v1/stores/{storeId}/profile/menus/{menuId}", NOT_FOUND_STORE_ID, UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_can_be_updated_and_menu_can_be_managed() throws Exception {
        String isolatedStoreId = createIsolatedStore();
        String updateBasicRequest = objectMapper.writeValueAsString(Map.of(
                "name", "모모커피 연남 테스트",
                "phone", "02-111-2222",
                "introduction", "테스트 소개",
                "address", "서울 마포구 테스트로 1",
                "category", "카페",
                "toneOfVoice", "친근한 말투"
        ));

        mockMvc.perform(authed(put("/v1/stores/{storeId}/profile/basic", isolatedStoreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBasicRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("모모커피 연남 테스트"))
                .andExpect(jsonPath("$.phone").value("02-111-2222"));

        String updateHoursRequest = objectMapper.writeValueAsString(Map.of(
                "businessHours", List.of(
                        Map.of("dayOfWeek", "MON", "openTime", "09:00", "closeTime", "20:00", "open", true),
                        Map.of("dayOfWeek", "TUE", "openTime", "09:00", "closeTime", "20:00", "open", true),
                        Map.of("dayOfWeek", "WED", "openTime", "09:00", "closeTime", "20:00", "open", true),
                        Map.of("dayOfWeek", "THU", "openTime", "09:00", "closeTime", "20:00", "open", true),
                        Map.of("dayOfWeek", "FRI", "openTime", "09:00", "closeTime", "21:00", "open", true),
                        Map.of("dayOfWeek", "SAT", "openTime", "10:00", "closeTime", "21:00", "open", true),
                        Map.of("dayOfWeek", "SUN", "openTime", "10:00", "closeTime", "19:00", "open", true)
                )
        ));

        mockMvc.perform(authed(put("/v1/stores/{storeId}/profile/hours", isolatedStoreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateHoursRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessHours.length()").value(7))
                .andExpect(jsonPath("$.businessHours[0].openTime").value("09:00"));

        String addMenuRequest = objectMapper.writeValueAsString(Map.of(
                "name", "테스트 메뉴",
                "description", "테스트 설명"
        ));

        String addMenuResponse = mockMvc.perform(authed(post("/v1/stores/{storeId}/profile/menus", isolatedStoreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addMenuRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayNode menuItems = (ArrayNode) objectMapper.readTree(addMenuResponse).get("menuItems");
        String addedMenuId = findMenuIdByName(menuItems, "테스트 메뉴");

        mockMvc.perform(authed(delete("/v1/stores/{storeId}/profile/menus/{menuId}", isolatedStoreId, addedMenuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuItems").isArray());
    }

    @Test
    void store_profile_update_for_unknown_store_returns_404() throws Exception {
        String updateBasicRequest = objectMapper.writeValueAsString(Map.of(
                "name", "없는 매장",
                "phone", "02-000-0000",
                "introduction", "없음",
                "address", "없음",
                "category", "없음",
                "toneOfVoice", "없음"
        ));

        mockMvc.perform(authed(put("/v1/stores/{storeId}/profile/basic", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBasicRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void review_summary_and_ai_reply_flow_work() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews/summary", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("MOCK_MAP"))
                .andExpect(jsonPath("$.externalTotalReviewCount").value(greaterThan(0)));

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-draft", FIFTH_REVIEW_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.source").value("TEMPLATE"));

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-auto-reply", FIFTH_REVIEW_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty())
                .andExpect(jsonPath("$.repliedAt").isNotEmpty());

        String replyRequest = objectMapper.writeValueAsString(Map.of(
                "content", "소중한 리뷰 감사합니다. 더 좋은 경험으로 보답하겠습니다."
        ));

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("소중한 리뷰 감사합니다. 더 좋은 경험으로 보답하겠습니다."))
                .andExpect(jsonPath("$.repliedAt").isNotEmpty());
    }

    @Test
    void ai_reply_for_unknown_review_returns_404() throws Exception {
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-draft", NOT_FOUND_REVIEW_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-auto-reply", NOT_FOUND_REVIEW_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", NOT_FOUND_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "답글")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void reviews_can_be_searched_by_keyword() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews", STORE_ID)
                        .param("keyword", "카페")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews", STORE_ID)
                        .param("keyword", "ZZZ_NOT_EXIST_KEYWORD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void review_summary_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews/summary", NOT_FOUND_STORE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void reviews_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews", NOT_FOUND_STORE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void mockmap_sync_and_review_inbox_return_502_when_mockmap_unavailable() throws Exception {
        mockMvc.perform(authed(post("/v1/stores/{storeId}/reviews/sync-mockmap", STORE_ID)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MOCK_MAP_API_ERROR"));

        mockMvc.perform(authed(get("/v1/reviews/inbox")
                        .param("placeIds", "ChIJN1t_tDeuEmsRUsoyG83frY4")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MOCK_MAP_API_ERROR"));
    }

    @Test
    void mockmap_sync_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(authed(post("/v1/stores/{storeId}/reviews/sync-mockmap", NOT_FOUND_STORE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void review_inbox_without_place_ids_returns_400() throws Exception {
        mockMvc.perform(authed(get("/v1/reviews/inbox")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void instagram_publish_returns_400_when_credentials_missing() throws Exception {
        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "first.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "second.jpg",
                "image/jpeg",
                new byte[]{4, 5, 6}
        );

        MockMultipartHttpServletRequestBuilder request = multipart("/v1/stores/{storeId}/contents/instagram/publish-carousel", STORE_ID)
                .file(image1)
                .file(image2);

        mockMvc.perform(authed(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void instagram_publish_requires_at_least_one_image() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/v1/stores/{storeId}/contents/instagram/publish-carousel", STORE_ID);

        mockMvc.perform(authed(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void instagram_publish_rejects_non_image_file() throws Exception {
        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "first.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "not-image.txt",
                "text/plain",
                "not-image".getBytes()
        );

        MockMultipartHttpServletRequestBuilder request = multipart("/v1/stores/{storeId}/contents/instagram/publish-carousel", STORE_ID)
                .file(image1)
                .file(image2);

        mockMvc.perform(authed(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void ai_suggest_mode_returns_title_and_body_without_creating_content() throws Exception {
        int beforeCount = objectMapper.readTree(
                        mockMvc.perform(authed(get("/v1/stores/{storeId}/contents", STORE_ID)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString()
                )
                .size();

        String suggestRequest = objectMapper.writeValueAsString(Map.of(
                "aiSuggest", true,
                "eventText", "장마 기간 따뜻한 라떼 할인 이벤트"
        ));

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suggestRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.body").isNotEmpty())
                .andExpect(jsonPath("$.source").value("TEMPLATE"))
                .andExpect(jsonPath("$.status").doesNotExist());

        int afterCount = objectMapper.readTree(
                        mockMvc.perform(authed(get("/v1/stores/{storeId}/contents", STORE_ID)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString()
                )
                .size();
        Assertions.assertEquals(beforeCount, afterCount);
    }

    @Test
    void content_create_without_title_and_body_returns_400() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "channels", List.of("Instagram")
        ));

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void invalid_token_returns_401() throws Exception {
        mockMvc.perform(get("/v1/stores")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protected_endpoints_without_auth_return_401() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/dashboard", STORE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/v1/stores/{storeId}/contents", STORE_ID))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/stores/{storeId}/profile", STORE_ID))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/stores/{storeId}/reviews", STORE_ID))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/reviews/inbox").param("placeIds", "ChIJN1t_tDeuEmsRUsoyG83frY4"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void non_member_cannot_access_seed_store() throws Exception {
        String outsiderToken = signUpAndLogin("outsider-" + UUID.randomUUID() + "@business.kr");

        mockMvc.perform(authedAs(outsiderToken, get("/v1/stores/{storeId}/dashboard", STORE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(authedAs(outsiderToken, get("/v1/stores/{storeId}/profile", STORE_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(authedAs(outsiderToken, get("/v1/stores/{storeId}/contents", STORE_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(authedAs(outsiderToken, post("/v1/reviews/{reviewId}/reply", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "무단 답글")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void stores_list_only_contains_memberships_for_current_user() throws Exception {
        String outsiderToken = signUpAndLogin("isolated-" + UUID.randomUUID() + "@business.kr");

        mockMvc.perform(authedAs(outsiderToken, get("/v1/stores")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(authed(get("/v1/stores")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test
    void review_inbox_rejects_unauthorized_place_id() throws Exception {
        mockMvc.perform(authed(get("/v1/reviews/inbox")
                        .param("placeIds", "ChIJ_UNAUTHORIZED_PLACE_ID")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void review_reply_with_empty_content_returns_400() throws Exception {
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void review_reply_can_be_deleted_and_re_registered() throws Exception {
        mockMvc.perform(authed(delete("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)));

        String replyRequest = objectMapper.writeValueAsString(Map.of("content", "삭제 테스트용 답글"));
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("삭제 테스트용 답글"));

        mockMvc.perform(authed(delete("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)))
                .andExpect(status().isOk());

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "다시 등록한 답글")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("다시 등록한 답글"));
    }

    @Test
    void delete_reply_without_existing_reply_returns_400() throws Exception {
        mockMvc.perform(authed(delete("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)));
        mockMvc.perform(authed(delete("/v1/reviews/{reviewId}/reply", THIRD_REVIEW_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void delete_reply_for_unknown_review_returns_404() throws Exception {
        mockMvc.perform(authed(delete("/v1/reviews/{reviewId}/reply", NOT_FOUND_REVIEW_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicate_review_reply_is_rejected() throws Exception {
        String firstReply = objectMapper.writeValueAsString(Map.of("content", "첫 번째 답글입니다."));
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", SECOND_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstReply)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("첫 번째 답글입니다."));

        String secondReply = objectMapper.writeValueAsString(Map.of("content", "두 번째 답글 시도"));
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", SECOND_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondReply)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void store_profile_update_with_null_fields_returns_400() throws Exception {
        String isolatedStoreId = createIsolatedStore();
        ObjectNode updateBasicRequest = objectMapper.createObjectNode();
        updateBasicRequest.putNull("name");
        updateBasicRequest.putNull("phone");
        updateBasicRequest.putNull("introduction");
        updateBasicRequest.putNull("address");
        updateBasicRequest.putNull("category");
        updateBasicRequest.putNull("toneOfVoice");

        mockMvc.perform(authed(put("/v1/stores/{storeId}/profile/basic", isolatedStoreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBasicRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void store_profile_remove_nonexistent_menu_returns_404() throws Exception {
        mockMvc.perform(authed(delete("/v1/stores/{storeId}/profile/menus/{menuId}", STORE_ID, UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void invalid_content_status_transition_returns_400() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "상태 전이 테스트",
                "body", "상태 전이 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "FAILED"));

        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void review_summary_exposes_external_and_local_counts_separately() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews/summary", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalTotalReviewCount").value(128))
                .andExpect(jsonPath("$.localSyncedReviewCount").value(5));
    }

    @Test
    void mockmap_oauth_endpoints_require_auth() throws Exception {
        mockMvc.perform(get("/api/integrations/mockmap/oauth/status").param("storeId", STORE_ID))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/integrations/mockmap/oauth/credentials")
                        .param("storeId", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientId", "test-client",
                                "clientSecret", "test-secret"
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/integrations/mockmap/oauth/credentials").param("storeId", STORE_ID))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/integrations/mockmap/oauth/disconnect").param("storeId", STORE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mockmap_oauth_credentials_lifecycle_works_without_external_server() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "clientId", "mock-client-" + UUID.randomUUID(),
                "clientSecret", "mock-secret-" + UUID.randomUUID()
        ));

        mockMvc.perform(authed(put("/api/integrations/mockmap/oauth/credentials")
                        .param("storeId", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialsConfigured").value(true))
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(authed(get("/api/integrations/mockmap/oauth/status").param("storeId", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialsConfigured").value(true));

        mockMvc.perform(authed(post("/api/integrations/mockmap/oauth/disconnect").param("storeId", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(authed(delete("/api/integrations/mockmap/oauth/credentials").param("storeId", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialsConfigured").value(false));
    }

    @Test
    void mockmap_oauth_save_credentials_with_missing_values_returns_bad_request() throws Exception {
        mockMvc.perform(authed(put("/api/integrations/mockmap/oauth/credentials")
                        .param("storeId", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientId", "",
                                "clientSecret", ""
                        )))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void mockmap_oauth_callback_without_code_returns_bad_request_html() throws Exception {
        mockMvc.perform(get("/api/integrations/mockmap/oauth/callback")
                        .param("state", "invalid-state"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("authorization code")));
    }

    @Test
    void mockmap_oauth_callback_with_error_param_returns_bad_request_html() throws Exception {
        mockMvc.perform(get("/api/integrations/mockmap/oauth/callback")
                        .param("error", "access_denied"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("access_denied")));
    }

    @Test
    void mockmap_oauth_start_without_credentials_returns_bad_gateway() throws Exception {
        String isolatedStoreId = createIsolatedStore();

        mockMvc.perform(authed(get("/api/integrations/mockmap/oauth/start").param("storeId", isolatedStoreId)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MOCK_MAP_API_ERROR"));
    }

    @Test
    void signup_with_missing_fields_returns_400() throws Exception {
        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "incomplete-" + UUID.randomUUID() + "@business.kr"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void instagram_publish_for_unknown_store_returns_404() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(authed(multipart("/v1/stores/{storeId}/contents/instagram/publish-carousel", NOT_FOUND_STORE_ID)
                        .file(image)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_create_for_unknown_store_returns_404() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "title", "없는 매장 콘텐츠",
                "body", "본문"
        ));

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void login_with_missing_fields_returns_400() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "name@business.kr"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void auth_response_includes_access_token_and_expires_at() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "name@business.kr",
                                "password", "Passw0rd!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("모모커피 운영자"));
    }

    @Test
    void created_store_is_immediately_accessible_to_creator() throws Exception {
        String isolatedStoreId = createIsolatedStore();

        mockMvc.perform(authed(get("/v1/stores/{storeId}/profile", isolatedStoreId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(isolatedStoreId));

        mockMvc.perform(authed(get("/v1/stores/{storeId}/dashboard", isolatedStoreId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(isolatedStoreId));
    }

    @Test
    void contents_can_be_filtered_by_each_seed_status() throws Exception {
        for (String status : List.of("PUBLISHED", "SCHEDULED", "FAILED", "DRAFT")) {
            String response = mockMvc.perform(authed(get("/v1/stores/{storeId}/contents", STORE_ID)
                            .param("status", status)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            ArrayNode items = (ArrayNode) objectMapper.readTree(response);
            for (int i = 0; i < items.size(); i++) {
                Assertions.assertEquals(status, items.get(i).get("status").asText());
            }
        }
    }

    @Test
    void content_status_can_transition_draft_to_scheduled() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "예약 게시 테스트",
                "body", "예약 게시 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "SCHEDULED"));

        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void failed_content_can_be_retried_to_draft() throws Exception {
        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents/{contentId}/retry", STORE_ID, FAILED_CONTENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FAILED_CONTENT_ID))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void published_content_retry_returns_400() throws Exception {
        String contentId = createAndPublishContent();

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents/{contentId}/retry", STORE_ID, contentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void published_content_cannot_be_changed_via_status_patch() throws Exception {
        String contentId = createAndPublishContent();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "DRAFT"));

        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void duplicate_ai_auto_reply_is_rejected() throws Exception {
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-auto-reply", FOURTH_REVIEW_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty());

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-auto-reply", FOURTH_REVIEW_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void ai_draft_for_replied_review_returns_400() throws Exception {
        String replyRequest = objectMapper.writeValueAsString(Map.of("content", "이미 답글을 남긴 리뷰입니다."));
        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/reply", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(authed(post("/v1/reviews/{reviewId}/ai-draft", REVIEW_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void logout_revokes_access_token() throws Exception {
        String email = "logout-user-" + UUID.randomUUID() + "@business.kr";
        String token = signUpAndLogin(email);

        mockMvc.perform(authedAs(token, get("/v1/stores")))
                .andExpect(status().isOk());

        mockMvc.perform(authedAs(token, post("/v1/auth/logout")))
                .andExpect(status().isNoContent());

        mockMvc.perform(authedAs(token, get("/v1/stores")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void mockmap_sync_without_place_id_returns_400() throws Exception {
        String isolatedStoreId = createIsolatedStore();

        mockMvc.perform(authed(post("/v1/stores/{storeId}/reviews/sync-mockmap", isolatedStoreId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void mockmap_oauth_non_member_store_returns_403() throws Exception {
        String outsiderToken = signUpAndLogin("oauth-outsider-" + UUID.randomUUID() + "@business.kr");

        mockMvc.perform(authedAs(outsiderToken, get("/api/integrations/mockmap/oauth/status").param("storeId", STORE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void reviews_are_returned_in_reviewed_at_descending_order() throws Exception {
        String response = mockMvc.perform(authed(get("/v1/stores/{storeId}/reviews", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayNode reviews = (ArrayNode) objectMapper.readTree(response);
        for (int i = 1; i < reviews.size(); i++) {
            Instant previous = Instant.parse(reviews.get(i - 1).get("reviewedAt").asText());
            Instant current = Instant.parse(reviews.get(i).get("reviewedAt").asText());
            Assertions.assertFalse(current.isAfter(previous),
                    "리뷰 목록은 reviewedAt 내림차순이어야 합니다.");
        }
    }

    @Test
    void dashboard_includes_review_widget_and_recent_reviews() throws Exception {
        mockMvc.perform(authed(get("/v1/stores/{storeId}/dashboard", STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewWidget.totalReviews").value(128))
                .andExpect(jsonPath("$.reviewWidget.syncedRecentReviews").value(5))
                .andExpect(jsonPath("$.reviewWidget.reviewListUrl").value("/v1/stores/" + STORE_ID + "/reviews"))
                .andExpect(jsonPath("$.recentReviews.length()").value(2))
                .andExpect(jsonPath("$.suggestionCard.title").isNotEmpty())
                .andExpect(jsonPath("$.checklistItems.length()").value(3))
                .andExpect(jsonPath("$.checklistItems[?(@.key=='check-hours')].completed").value(true))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test
    void non_member_cannot_update_seed_store_profile() throws Exception {
        String outsiderToken = signUpAndLogin("profile-outsider-" + UUID.randomUUID() + "@business.kr");
        String updateBasicRequest = objectMapper.writeValueAsString(Map.of(
                "name", "무단 수정",
                "phone", "02-000-0000",
                "introduction", "무단",
                "address", "무단",
                "category", "무단",
                "toneOfVoice", "무단"
        ));

        mockMvc.perform(authedAs(outsiderToken, put("/v1/stores/{storeId}/profile/basic", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBasicRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void content_create_defaults_channels_to_instagram() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "채널 기본값 테스트",
                "body", "채널 기본값 본문"
        ));

        mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channels[0]").value("Instagram"));
    }

    @Test
    void endpoint_inventory_matches_expected_contract() {
        Set<String> actualEndpoints = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("org.hackathon12.shophub.infrastructure.web"))
                .flatMap(entry -> {
                    Set<String> paths = entry.getKey().getPatternValues();
                    Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
                    if (methods.isEmpty()) {
                        return paths.stream().map(path -> "ANY " + path);
                    }
                    return methods.stream().flatMap(method -> paths.stream().map(path -> method.name() + " " + path));
                })
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> expectedEndpoints = new TreeSet<>(Set.of(
                "DELETE /api/integrations/mockmap/oauth/credentials",
                "DELETE /v1/reviews/{reviewId}/reply",
                "DELETE /v1/stores/{storeId}/profile/menus/{menuId}",
                "GET /api/integrations/mockmap/oauth/callback",
                "GET /api/integrations/mockmap/oauth/start",
                "GET /api/integrations/mockmap/oauth/status",
                "GET /v1/reviews/inbox",
                "GET /v1/stores",
                "GET /v1/stores/{storeId}/contents",
                "GET /v1/stores/{storeId}/dashboard",
                "GET /v1/stores/{storeId}/profile",
                "GET /v1/stores/{storeId}/reviews",
                "GET /v1/stores/{storeId}/reviews/summary",
                "PATCH /v1/stores/{storeId}/contents/{contentId}/status",
                "POST /api/integrations/mockmap/oauth/disconnect",
                "POST /v1/auth/login",
                "POST /v1/auth/logout",
                "POST /v1/auth/signup",
                "POST /v1/reviews/{reviewId}/ai-auto-reply",
                "POST /v1/reviews/{reviewId}/ai-draft",
                "POST /v1/reviews/{reviewId}/reply",
                "POST /v1/stores",
                "POST /v1/stores/{storeId}/contents",
                "POST /v1/stores/{storeId}/contents/{contentId}/retry",
                "POST /v1/stores/{storeId}/contents/instagram/publish-carousel",
                "POST /v1/stores/{storeId}/profile/menus",
                "POST /v1/stores/{storeId}/reviews/sync-mockmap",
                "PUT /api/integrations/mockmap/oauth/credentials",
                "PUT /v1/stores/{storeId}/profile/basic",
                "PUT /v1/stores/{storeId}/profile/hours"
        ));

        Assertions.assertEquals(expectedEndpoints, actualEndpoints);
    }

    private String createIsolatedStore() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "OAuth 테스트 매장 " + UUID.randomUUID(),
                "phone", "02-9999-8888",
                "introduction", "OAuth 테스트",
                "address", "서울 테스트구 OAuth로 1",
                "category", "카페",
                "toneOfVoice", "테스트 말투"
        ));

        String response = mockMvc.perform(authed(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createAndPublishContent() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "게시 상태 테스트 " + UUID.randomUUID(),
                "body", "게시 상태 테스트 본문"
        ));

        String createdJson = mockMvc.perform(authed(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));

        mockMvc.perform(authed(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        return contentId;
    }

    private String findMenuIdByName(ArrayNode menuItems, String menuName) {
        for (int i = 0; i < menuItems.size(); i++) {
            ObjectNode menu = (ObjectNode) menuItems.get(i);
            if (menuName.equals(menu.get("name").asText())) {
                return menu.get("id").asText();
            }
        }
        throw new IllegalStateException("추가된 메뉴를 찾지 못했습니다. menuName=" + menuName);
    }
}
