package org.hackathon12.shophub.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthSessionProperties.class)
public class AuthSessionConfig {
}
