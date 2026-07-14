package org.hackathon12.shophub.infrastructure.web.integration;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.hackathon12.shophub.domain.integration.service.OAuthIntegrationService;
import org.hackathon12.shophub.infrastructure.mockmap.MockMapApiException;
import org.hackathon12.shophub.infrastructure.x.XApiException;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
public class OAuthIntegrationController {

    private final OAuthIntegrationService oauthIntegrationService;
    private final ShopHubAuthGuard shopHubAuthGuard;

    public OAuthIntegrationController(
            OAuthIntegrationService oauthIntegrationService,
            ShopHubAuthGuard shopHubAuthGuard
    ) {
        this.oauthIntegrationService = oauthIntegrationService;
        this.shopHubAuthGuard = shopHubAuthGuard;
    }

    @GetMapping("/oauth/status")
    public List<OAuthConnectionStatus> listStatuses(
            @RequestParam UUID storeId,
            HttpServletRequest request
    ) {
        shopHubAuthGuard.requireStoreMember(request, storeId);
        return oauthIntegrationService.listConnectionStatuses(storeId);
    }

    @PutMapping("/{type}/oauth/credentials")
    public OAuthConnectionStatus saveCredentials(
            @PathVariable String type,
            @RequestParam UUID storeId,
            @RequestBody SaveCredentialsRequest request,
            HttpServletRequest httpRequest
    ) {
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(type);
        UUID userId = shopHubAuthGuard.requireStoreMember(httpRequest, storeId);
        return provider.saveCredentials(storeId, userId, request.clientId(), request.clientSecret());
    }

    @DeleteMapping("/{type}/oauth/credentials")
    public OAuthConnectionStatus deleteCredentials(
            @PathVariable String type,
            @RequestParam UUID storeId,
            HttpServletRequest httpRequest
    ) {
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(type);
        UUID userId = shopHubAuthGuard.requireStoreMember(httpRequest, storeId);
        return provider.deleteCredentials(storeId, userId);
    }

    @GetMapping("/{type}/oauth/start")
    public void startOAuth(
            @PathVariable String type,
            @RequestParam UUID storeId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(type);
        UUID userId = shopHubAuthGuard.requireStoreMember(request, storeId);
        response.sendRedirect(provider.buildAuthorizationUrl(storeId, userId));
    }

    @GetMapping("/{type}/oauth/callback")
    public ResponseEntity<String> callback(
            @PathVariable String type,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        OAuthIntegrationType integrationType = OAuthIntegrationType.fromPathValue(type);
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(integrationType);

        if (StringUtils.hasText(error)) {
            return htmlResponse(
                    HttpStatus.BAD_REQUEST,
                    integrationType.pathValue() + " OAuth 연결 실패",
                    integrationType.pathValue() + "에서 오류가 반환되었습니다: " + error
            );
        }

        try {
            OAuthConnectionStatus status = provider.completeAuthorization(code, state);
            return htmlResponse(
                    HttpStatus.OK,
                    integrationType.pathValue() + " OAuth 연결 완료",
                    "연동 타입: " + integrationType.pathValue() + "<br>"
                            + "가게: " + status.placeName() + " (" + status.placeId() + ")<br>"
                            + "Client ID: " + status.clientId() + "<br>"
                            + "ShopHub BE가 이제 이 가게의 " + integrationType.pathValue() + " 답글 API를 사용할 수 있습니다."
            );
        } catch (MockMapApiException exception) {
            return htmlResponse(
                    HttpStatus.BAD_REQUEST,
                    integrationType.pathValue() + " OAuth 연결 실패",
                    exception.getMessage()
            );
        } catch (XApiException exception) {
            return htmlResponse(
                    HttpStatus.BAD_REQUEST,
                    integrationType.pathValue() + " OAuth 연결 실패",
                    exception.getMessage()
            );
        }
    }

    @PostMapping("/{type}/oauth/disconnect")
    public OAuthConnectionStatus disconnect(
            @PathVariable String type,
            @RequestParam UUID storeId,
            HttpServletRequest httpRequest
    ) {
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(type);
        UUID userId = shopHubAuthGuard.requireStoreMember(httpRequest, storeId);
        return provider.disconnect(storeId, userId);
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
