package org.hackathon12.shophub.infrastructure.x;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.review-sync.enabled=false",
        "mock-map.api.base-url=http://127.0.0.1:59999",
        "x.oauth.client-id=test-x-client",
        "x.oauth.client-secret=test-x-secret",
        "openai.api-key="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XOAuthIntegrationTest {

    private static final String STORE_ID = "0f7ed494-8e0e-4c5e-b6f4-5294ee3989d1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void twitter_oauth_start_redirects_when_app_credentials_configured() throws Exception {
        String token = loginAsSeedUser();

        mockMvc.perform(get("/api/integrations/X/oauth/start")
                        .param("storeId", STORE_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("twitter.com/i/oauth2/authorize")));
    }

    @Test
    void twitter_oauth_callback_without_code_returns_bad_request_html() throws Exception {
        mockMvc.perform(get("/api/integrations/X/oauth/callback")
                        .param("state", "invalid-state"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("authorization code")));
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
