package org.hackathon12.shophub.infrastructure.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.review-sync.enabled=false",
        "mock-map.api.base-url=http://127.0.0.1:59999",
        "instagram.graph.account-id=test-instagram-account",
        "instagram.graph.access-token=test-instagram-token",
        "instagram.graph.allowed-account-id=test-instagram-account",
        "instagram.graph.allowed-username=commentcopybot",
        "instagram.oauth.redirect-uri=http://localhost:8080/api/integrations/INSTAGRAM/oauth/callback",
        "facebook.graph.page-id=test-facebook-page",
        "facebook.graph.access-token=test-facebook-token",
        "facebook.graph.allowed-page-id=test-facebook-page",
        "facebook.graph.allowed-page-name=commentcopybot",
        "facebook.oauth.redirect-uri=http://localhost:8080/api/integrations/FACEBOOK/oauth/callback",
        "openai.api-key="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FixedGraphAccountIntegrationStatusTest {

    private static final String STORE_ID = "0f7ed494-8e0e-4c5e-b6f4-5294ee3989d1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void oauth_status_exposes_fixed_instagram_and_facebook_accounts_before_connect() throws Exception {
        String token = loginAsSeedUser();

        mockMvc.perform(get("/api/integrations/oauth/status")
                        .param("storeId", STORE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='INSTAGRAM')].credentialsConfigured").value(true))
                .andExpect(jsonPath("$[?(@.type=='INSTAGRAM')].connected").value(false))
                .andExpect(jsonPath("$[?(@.type=='FACEBOOK')].credentialsConfigured").value(true))
                .andExpect(jsonPath("$[?(@.type=='FACEBOOK')].connected").value(false));
    }

    @Test
    @Order(2)
    void instagram_connect_links_store_to_configured_account() throws Exception {
        String token = loginAsSeedUser();

        String authorizeUrl = mockMvc.perform(get("/api/integrations/INSTAGRAM/oauth/authorize-url")
                        .param("storeId", STORE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"authorizationUrl\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get(authorizeUrl))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("success=true")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("type=INSTAGRAM")));

        mockMvc.perform(get("/api/integrations/oauth/status")
                        .param("storeId", STORE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='INSTAGRAM')].connected").value(true))
                .andExpect(jsonPath("$[?(@.type=='INSTAGRAM')].placeName").value("@commentcopybot"));
    }

    @Test
    @Order(3)
    void facebook_connect_links_store_to_configured_page() throws Exception {
        String token = loginAsSeedUser();

        String authorizeUrl = mockMvc.perform(get("/api/integrations/FACEBOOK/oauth/authorize-url")
                        .param("storeId", STORE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"authorizationUrl\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get(authorizeUrl))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("success=true")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("type=FACEBOOK")));

        mockMvc.perform(get("/api/integrations/oauth/status")
                        .param("storeId", STORE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='FACEBOOK')].connected").value(true))
                .andExpect(jsonPath("$[?(@.type=='FACEBOOK')].placeName").value("commentcopybot"));
    }

    private String loginAsSeedUser() throws Exception {
        String response = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"name@business.kr","password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }
}
