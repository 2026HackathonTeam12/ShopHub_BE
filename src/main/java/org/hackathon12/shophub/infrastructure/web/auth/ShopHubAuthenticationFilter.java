package org.hackathon12.shophub.infrastructure.web.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hackathon12.shophub.global.error.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class ShopHubAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/v1/auth/",
            "/api/integrations/mockmap/oauth/callback",
            "/v3/api-docs",
            "/swagger-ui",
            "/uploads/"
    );

    private final AuthContext authContext;
    private final ObjectMapper objectMapper;

    public ShopHubAuthenticationFilter(AuthContext authContext, ObjectMapper objectMapper) {
        this.authContext = authContext;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (authContext.resolveUserId(request).isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new GlobalExceptionHandler.ApiErrorResponse(
                        "UNAUTHORIZED",
                        "ShopHub 로그인이 필요합니다.",
                        Instant.now()
                )
        );
    }
}
