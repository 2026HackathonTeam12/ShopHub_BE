package org.hackathon12.shophub.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }
}
