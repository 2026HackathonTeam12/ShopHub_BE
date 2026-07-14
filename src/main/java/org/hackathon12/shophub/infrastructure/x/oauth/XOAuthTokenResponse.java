package org.hackathon12.shophub.infrastructure.x.oauth;

public record XOAuthTokenResponse(
        String access_token,
        String refresh_token,
        Integer expires_in,
        String token_type,
        String scope
) {
}
