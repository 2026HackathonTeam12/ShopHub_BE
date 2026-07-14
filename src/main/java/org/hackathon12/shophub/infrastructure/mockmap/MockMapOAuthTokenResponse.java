package org.hackathon12.shophub.infrastructure.mockmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MockMapOAuthTokenResponse(
        String access_token,
        String refresh_token,
        String token_type,
        Integer expires_in,
        Integer refresh_expires_in,
        String scope,
        String client_id,
        String place_id,
        String place_name
) {
}
