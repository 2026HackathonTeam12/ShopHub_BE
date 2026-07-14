package org.hackathon12.shophub.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.hackathon12.shophub.infrastructure.web.auth.AuthController;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "BearerAuth";

    @Bean
    public OpenAPI shopHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopHub API")
                        .version("0.2.0")
                        .description("""
                                ShopHub REST API.

                                ## 인증
                                대부분의 API는 `POST /v1/auth/login` 또는 `POST /v1/auth/signup` 으로 발급받은
                                `accessToken`을 **Authorize** 버튼에 입력하거나
                                `Authorization: Bearer {accessToken}` 헤더로 전달합니다.
                                """))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("accessToken")
                                .description("""
                                        login/signup 응답의 accessToken.
                                        Authorization: Bearer {accessToken}
                                        """)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OperationCustomizer publicAuthEndpointsCustomizer() {
        return (operation, handlerMethod) -> {
            if (isPublicAuthEndpoint(handlerMethod)) {
                operation.setSecurity(List.of());
            }
            return operation;
        };
    }

    private boolean isPublicAuthEndpoint(HandlerMethod handlerMethod) {
        if (!AuthController.class.equals(handlerMethod.getBeanType())) {
            return false;
        }

        String methodName = handlerMethod.getMethod().getName();
        return "signUp".equals(methodName) || "login".equals(methodName);
    }
}
