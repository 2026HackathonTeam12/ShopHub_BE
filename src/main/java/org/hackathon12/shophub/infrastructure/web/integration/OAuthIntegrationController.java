package org.hackathon12.shophub.infrastructure.web.integration;

import org.hackathon12.shophub.domain.integration.model.OAuthConnectionStatus;
import org.hackathon12.shophub.domain.integration.model.OAuthIntegrationType;
import org.hackathon12.shophub.domain.integration.port.OAuthIntegrationProvider;
import org.hackathon12.shophub.domain.integration.service.OAuthIntegrationService;
import org.hackathon12.shophub.global.config.FrontendProperties;
import org.hackathon12.shophub.infrastructure.mockmap.MockMapApiException;
import org.hackathon12.shophub.infrastructure.x.XApiException;
import org.hackathon12.shophub.infrastructure.web.auth.ShopHubAuthGuard;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
public class OAuthIntegrationController {

    private final OAuthIntegrationService oauthIntegrationService;
    private final ShopHubAuthGuard shopHubAuthGuard;
    private final FrontendProperties frontendProperties;

    public OAuthIntegrationController(
            OAuthIntegrationService oauthIntegrationService,
            ShopHubAuthGuard shopHubAuthGuard,
            FrontendProperties frontendProperties
    ) {
        this.oauthIntegrationService = oauthIntegrationService;
        this.shopHubAuthGuard = shopHubAuthGuard;
        this.frontendProperties = frontendProperties;
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

    @GetMapping("/{type}/oauth/authorize-url")
    public OAuthAuthorizeUrlResponse authorizeUrl(
            @PathVariable String type,
            @RequestParam UUID storeId,
            HttpServletRequest request
    ) {
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(type);
        UUID userId = shopHubAuthGuard.requireStoreMember(request, storeId);
        return new OAuthAuthorizeUrlResponse(provider.buildAuthorizationUrl(storeId, userId));
    }

    @GetMapping("/{type}/oauth/callback")
    public ResponseEntity<?> callback(
            @PathVariable String type,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        OAuthIntegrationType integrationType = OAuthIntegrationType.fromPathValue(type);
        OAuthIntegrationProvider provider = oauthIntegrationService.requireProvider(integrationType);

        if (StringUtils.hasText(error)) {
            return redirectToFrontend(
                    integrationType.pathValue(),
                    false,
                    integrationType.pathValue() + "에서 오류가 반환되었습니다: " + error
            );
        }

        try {
            OAuthConnectionStatus status = provider.completeAuthorization(code, state);
            String message = status.placeName() + " (" + status.placeId() + ") 연동이 완료되었습니다.";
            return redirectToFrontend(integrationType.pathValue(), true, message);
        } catch (MockMapApiException exception) {
            return redirectToFrontend(integrationType.pathValue(), false, exception.getMessage());
        } catch (XApiException exception) {
            return redirectToFrontend(integrationType.pathValue(), false, exception.getMessage());
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

    private ResponseEntity<Void> redirectToFrontend(String type, boolean success, String message) {
        if (!StringUtils.hasText(frontendProperties.baseUrl())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        URI location = UriComponentsBuilder
                .fromUriString(trimTrailingSlash(frontendProperties.baseUrl()))
                .path("/integrations/oauth/callback")
                .queryParam("type", type)
                .queryParam("success", success)
                .queryParam("message", message)
                .build()
                .encode()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record SaveCredentialsRequest(
            String clientId,
            String clientSecret
    ) {
    }

    public record OAuthAuthorizeUrlResponse(
            String authorizationUrl
    ) {
    }
}
