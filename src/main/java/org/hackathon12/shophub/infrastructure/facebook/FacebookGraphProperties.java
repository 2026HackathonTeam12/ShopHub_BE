package org.hackathon12.shophub.infrastructure.facebook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facebook.graph")
public record FacebookGraphProperties(
        String baseUrl,
        String pageId,
        String accessToken,
        String allowedPageId,
        String allowedPageName
) {
}
