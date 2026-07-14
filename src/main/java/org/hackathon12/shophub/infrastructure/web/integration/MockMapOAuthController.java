package org.hackathon12.shophub.infrastructure.web.integration;

import org.hackathon12.shophub.infrastructure.mockmap.MockMapApiException;
import org.hackathon12.shophub.infrastructure.mockmap.MockMapOAuthConnectionStatus;
import org.hackathon12.shophub.infrastructure.mockmap.MockMapOwnerOAuthService;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/mockmap/oauth")
@ConditionalOnProperty(prefix = "app.review-source", name = "provider", havingValue = "mockmap", matchIfMissing = true)
public class MockMapOAuthController {

    private final MockMapOwnerOAuthService mockMapOwnerOAuthService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public MockMapOAuthController(
            MockMapOwnerOAuthService mockMapOwnerOAuthService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.mockMapOwnerOAuthService = mockMapOwnerOAuthService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @PutMapping("/credentials")
    public MockMapOAuthConnectionStatus saveCredentials(
            @RequestParam UUID storeId,
            @RequestBody SaveCredentialsRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID userId = shopHubAuthGuard.requireStoreMember(httpRequest, storeId);
        return mockMapOwnerOAuthService.saveCredentials(
                storeId,
                userId,
                request.clientId(),
                request.clientSecret()
        );
    }

    @DeleteMapping("/credentials")
    public MockMapOAuthConnectionStatus deleteCredentials(
            @RequestParam UUID storeId,
            HttpServletRequest httpRequest
    ) {
        UUID userId = shopHubAuthGuard.requireStoreMember(httpRequest, storeId);
        return mockMapOwnerOAuthService.deleteCredentials(storeId, userId);
    }

    @GetMapping("/start")
    public void startOAuth(
            @RequestParam UUID storeId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        UUID userId = shopHubAuthGuard.requireStoreMember(request, storeId);
        response.sendRedirect(mockMapOwnerOAuthService.buildAuthorizationUrl(storeId, userId));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        if (StringUtils.hasText(error)) {
            return htmlResponse(
                    HttpStatus.BAD_REQUEST,
                    "MockMap OAuth 연결 실패",
                    "MockMap에서 오류가 반환되었습니다: " + error
            );
        }

        try {
            MockMapOAuthConnectionStatus status = mockMapOwnerOAuthService.completeAuthorization(code, state);
            return htmlResponse(
                    HttpStatus.OK,
                    "MockMap OAuth 연결 완료",
                    "가게: " + status.placeName() + " (" + status.placeId() + ")<br>"
                            + "Client ID: " + status.clientId() + "<br>"
                            + "ShopHub BE가 이제 이 가게의 MockMap 답글 API를 사용할 수 있습니다."
            );
        } catch (MockMapApiException exception) {
            return htmlResponse(HttpStatus.BAD_REQUEST, "MockMap OAuth 연결 실패", exception.getMessage());
        }
    }

    @GetMapping("/status")
    public MockMapOAuthConnectionStatus status(@RequestParam UUID storeId, HttpServletRequest request) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return mockMapOwnerOAuthService.getConnectionStatus(storeId);
    }

    @PostMapping("/disconnect")
    public MockMapOAuthConnectionStatus disconnect(
            @RequestParam UUID storeId,
            HttpServletRequest httpRequest
    ) {
        UUID userId = shopHubAuthGuard.requireStoreMember(httpRequest, storeId);
        return mockMapOwnerOAuthService.disconnect(storeId, userId);
    }

    private ResponseEntity<String> htmlResponse(HttpStatus status, String title, String message) {
        String html = """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <title>%s</title>
                </head>
                <body>
                  <h1>%s</h1>
                  <p>%s</p>
                </body>
                </html>
                """.formatted(title, title, message);
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(html);
    }

    public record SaveCredentialsRequest(
            String clientId,
            String clientSecret
    ) {
    }
}
