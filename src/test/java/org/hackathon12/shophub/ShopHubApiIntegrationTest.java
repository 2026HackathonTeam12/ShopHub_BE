package org.hackathon12.shophub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "google.places.api-key=",
        "instagram.graph.account-id=",
        "instagram.graph.access-token=",
        "openai.api-key="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopHubApiIntegrationTest {

    private static final String STORE_ID = "0f7ed494-8e0e-4c5e-b6f4-5294ee3989d1";
    private static final String NOT_FOUND_STORE_ID = "00000000-0000-0000-0000-000000000000";
    private static final String REVIEW_ID = "d2c39a88-6461-4f93-9f7f-f40103f5205c";
    private static final String NOT_FOUND_REVIEW_ID = "11111111-1111-1111-1111-111111111111";
    private static final String NOT_FOUND_CONTENT_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

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
    void login_with_invalid_credentials_returns_400() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "name@business.kr",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void dashboard_overview_can_be_loaded() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/dashboard", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(STORE_ID))
                .andExpect(jsonPath("$.draftContent.maxCharCount").value(2200))
                .andExpect(jsonPath("$.checklistItems.length()").value(greaterThan(0)));
    }

    @Test
    void dashboard_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/dashboard", NOT_FOUND_STORE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void draft_contents_can_be_filtered() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/contents", STORE_ID)
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void contents_can_be_loaded_without_filter() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/contents", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    @Test
    void content_status_can_be_changed() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "테스트 콘텐츠",
                "body", "테스트 본문"
        ));

        String createdJson = mockMvc.perform(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));

        mockMvc.perform(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void content_status_change_with_mismatched_store_returns_404() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "스토어 검증 콘텐츠",
                "body", "스토어 검증 본문"
        ));

        String createdJson = mockMvc.perform(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));

        mockMvc.perform(patch("/v1/stores/{storeId}/contents/{contentId}/status", NOT_FOUND_STORE_ID, contentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_status_change_for_unknown_content_returns_404() throws Exception {
        String patchRequest = objectMapper.writeValueAsString(Map.of("status", "PUBLISHED"));
        mockMvc.perform(patch("/v1/stores/{storeId}/contents/{contentId}/status", STORE_ID, NOT_FOUND_CONTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_can_be_retried() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "재시도 대상 콘텐츠",
                "body", "재시도 본문"
        ));

        String createdJson = mockMvc.perform(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/v1/stores/{storeId}/contents/{contentId}/retry", STORE_ID, contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contentId))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void content_retry_with_mismatched_store_returns_404() throws Exception {
        String createRequest = objectMapper.writeValueAsString(Map.of(
                "title", "재시도 스토어 검증 콘텐츠",
                "body", "재시도 스토어 검증 본문"
        ));

        String createdJson = mockMvc.perform(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/v1/stores/{storeId}/contents/{contentId}/retry", NOT_FOUND_STORE_ID, contentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void content_retry_for_unknown_content_returns_404() throws Exception {
        mockMvc.perform(post("/v1/stores/{storeId}/contents/{contentId}/retry", STORE_ID, NOT_FOUND_CONTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void stores_can_be_loaded() throws Exception {
        mockMvc.perform(get("/v1/stores"))
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

        mockMvc.perform(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
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

        mockMvc.perform(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
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

        mockMvc.perform(post("/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void store_profile_can_be_loaded() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/profile", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.menuItems.length()").value(greaterThan(0)));
    }

    @Test
    void store_profile_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/profile", NOT_FOUND_STORE_ID))
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

        mockMvc.perform(put("/v1/stores/{storeId}/profile/hours", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_add_menu_for_unknown_store_returns_404() throws Exception {
        String request = objectMapper.writeValueAsString(Map.of(
                "name", "없는 매장 메뉴",
                "description", "설명"
        ));

        mockMvc.perform(post("/v1/stores/{storeId}/profile/menus", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_remove_menu_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(delete("/v1/stores/{storeId}/profile/menus/{menuId}", NOT_FOUND_STORE_ID, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void store_profile_can_be_updated_and_menu_can_be_managed() throws Exception {
        String updateBasicRequest = objectMapper.writeValueAsString(Map.of(
                "name", "모모커피 연남 테스트",
                "phone", "02-111-2222",
                "introduction", "테스트 소개",
                "address", "서울 마포구 테스트로 1",
                "category", "카페",
                "toneOfVoice", "친근한 말투"
        ));

        mockMvc.perform(put("/v1/stores/{storeId}/profile/basic", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBasicRequest))
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

        mockMvc.perform(put("/v1/stores/{storeId}/profile/hours", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateHoursRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessHours.length()").value(7))
                .andExpect(jsonPath("$.businessHours[0].openTime").value("09:00"));

        String addMenuRequest = objectMapper.writeValueAsString(Map.of(
                "name", "테스트 메뉴",
                "description", "테스트 설명"
        ));

        String addMenuResponse = mockMvc.perform(post("/v1/stores/{storeId}/profile/menus", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addMenuRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ArrayNode menuItems = (ArrayNode) objectMapper.readTree(addMenuResponse).get("menuItems");
        String addedMenuId = findMenuIdByName(menuItems, "테스트 메뉴");

        mockMvc.perform(delete("/v1/stores/{storeId}/profile/menus/{menuId}", STORE_ID, addedMenuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuItems[*].name").isArray());
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

        mockMvc.perform(put("/v1/stores/{storeId}/profile/basic", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBasicRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void review_summary_and_ai_reply_flow_work() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/reviews/summary", STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("GOOGLE"))
                .andExpect(jsonPath("$.totalReviewCount").value(greaterThan(0)));

        mockMvc.perform(post("/v1/reviews/{reviewId}/ai-draft", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());

        mockMvc.perform(post("/v1/reviews/{reviewId}/ai-auto-reply", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty())
                .andExpect(jsonPath("$.repliedAt").isNotEmpty());

        String replyRequest = objectMapper.writeValueAsString(Map.of(
                "content", "소중한 리뷰 감사합니다. 더 좋은 경험으로 보답하겠습니다."
        ));

        mockMvc.perform(post("/v1/reviews/{reviewId}/reply", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replyRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("소중한 리뷰 감사합니다. 더 좋은 경험으로 보답하겠습니다."))
                .andExpect(jsonPath("$.repliedAt").isNotEmpty());
    }

    @Test
    void ai_reply_for_unknown_review_returns_404() throws Exception {
        mockMvc.perform(post("/v1/reviews/{reviewId}/ai-draft", NOT_FOUND_REVIEW_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(post("/v1/reviews/{reviewId}/ai-auto-reply", NOT_FOUND_REVIEW_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(post("/v1/reviews/{reviewId}/reply", NOT_FOUND_REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "답글"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void reviews_can_be_searched_by_keyword() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/reviews", STORE_ID)
                        .param("keyword", "카페"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        mockMvc.perform(get("/v1/stores/{storeId}/reviews", STORE_ID)
                        .param("keyword", "ZZZ_NOT_EXIST_KEYWORD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void review_summary_for_unknown_store_returns_404() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/reviews/summary", NOT_FOUND_STORE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void reviews_for_unknown_store_returns_empty_list() throws Exception {
        mockMvc.perform(get("/v1/stores/{storeId}/reviews", NOT_FOUND_STORE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void google_sync_and_review_inbox_return_502_when_api_key_is_missing() throws Exception {
        String syncRequest = objectMapper.writeValueAsString(Map.of("limit", 3));

        mockMvc.perform(post("/v1/stores/{storeId}/reviews/sync-google", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncRequest))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GOOGLE_PLACES_API_ERROR"));

        mockMvc.perform(get("/v1/reviews/inbox")
                        .param("placeIds", "ChIJN1t_tDeuEmsRUsoyG83frY4"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GOOGLE_PLACES_API_ERROR"));
    }

    @Test
    void google_sync_for_unknown_store_returns_404() throws Exception {
        String syncRequest = objectMapper.writeValueAsString(Map.of("limit", 3));

        mockMvc.perform(post("/v1/stores/{storeId}/reviews/sync-google", NOT_FOUND_STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void review_inbox_without_place_ids_returns_400() throws Exception {
        mockMvc.perform(get("/v1/reviews/inbox"))
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

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void instagram_publish_requires_at_least_one_image() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/v1/stores/{storeId}/contents/instagram/publish-carousel", STORE_ID);

        mockMvc.perform(request)
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

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void ai_suggest_mode_returns_title_and_body_without_creating_content() throws Exception {
        int beforeCount = objectMapper.readTree(
                        mockMvc.perform(get("/v1/stores/{storeId}/contents", STORE_ID))
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

        mockMvc.perform(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suggestRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.body").isNotEmpty())
                .andExpect(jsonPath("$.status").doesNotExist());

        int afterCount = objectMapper.readTree(
                        mockMvc.perform(get("/v1/stores/{storeId}/contents", STORE_ID))
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

        mockMvc.perform(post("/v1/stores/{storeId}/contents", STORE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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
                "DELETE /v1/stores/{storeId}/profile/menus/{menuId}",
                "GET /v1/reviews/inbox",
                "GET /v1/stores",
                "GET /v1/stores/{storeId}/contents",
                "GET /v1/stores/{storeId}/dashboard",
                "GET /v1/stores/{storeId}/profile",
                "GET /v1/stores/{storeId}/reviews",
                "GET /v1/stores/{storeId}/reviews/summary",
                "PATCH /v1/stores/{storeId}/contents/{contentId}/status",
                "POST /v1/auth/login",
                "POST /v1/auth/signup",
                "POST /v1/reviews/{reviewId}/ai-auto-reply",
                "POST /v1/reviews/{reviewId}/ai-draft",
                "POST /v1/reviews/{reviewId}/reply",
                "POST /v1/stores",
                "POST /v1/stores/{storeId}/contents",
                "POST /v1/stores/{storeId}/contents/{contentId}/retry",
                "POST /v1/stores/{storeId}/contents/instagram/publish-carousel",
                "POST /v1/stores/{storeId}/profile/menus",
                "POST /v1/stores/{storeId}/reviews/sync-google",
                "PUT /v1/stores/{storeId}/profile/basic",
                "PUT /v1/stores/{storeId}/profile/hours"
        ));

        Assertions.assertEquals(expectedEndpoints, actualEndpoints);
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
