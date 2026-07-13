package org.hackathon12.shophub.global.config;

import org.hackathon12.shophub.infrastructure.storage.LocalImageStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final LocalImageStorageService localImageStorageService;

    public StaticResourceConfig(LocalImageStorageService localImageStorageService) {
        this.localImageStorageService = localImageStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(localImageStorageService.fileStorageLocation());
    }
}
