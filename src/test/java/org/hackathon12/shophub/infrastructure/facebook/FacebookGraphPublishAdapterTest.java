package org.hackathon12.shophub.infrastructure.facebook;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacebookGraphPublishAdapterTest {

    @Test
    void buildFeedParams_usesIndexedAttachedMediaKeys() {
        FacebookGraphPublishAdapter adapter = new FacebookGraphPublishAdapter(
                RestClient.builder(),
                new FacebookGraphProperties(
                        "https://graph.facebook.com/v21.0",
                        "123",
                        "token",
                        "123",
                        "page"
                )
        );

        Map<String, String> params = adapter.buildFeedParams("hello", List.of("photo-1", "photo-2"));

        assertEquals("hello", params.get("message"));
        assertEquals("{\"media_fbid\":\"photo-1\"}", params.get("attached_media[0]"));
        assertEquals("{\"media_fbid\":\"photo-2\"}", params.get("attached_media[1]"));
        assertTrue(params.keySet().stream().noneMatch(key -> key.equals("attached_media")));
    }
}
