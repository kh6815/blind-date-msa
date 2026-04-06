package com.project.blinddate.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // HTML 페이지 경로
        registry.addMapping("/chats/**")
                .allowedOriginPatterns("http://*.blind-date.site", "http://*.blind-date.com", "https://*.blind-date.site", "https://*.blind-date.com")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);

        // REST API 경로
        registry.addMapping("/api/v1/chats/**")
                .allowedOriginPatterns("http://*.blind-date.site", "http://*.blind-date.com", "https://*.blind-date.site", "https://*.blind-date.com")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);

        // WebSocket 경로
        registry.addMapping("/ws/**")
                .allowedOriginPatterns("http://*.blind-date.site", "http://*.blind-date.com", "https://*.blind-date.site", "https://*.blind-date.com")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
