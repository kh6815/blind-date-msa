package com.project.blinddate.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UserIdArgumentResolver());
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // trailing slash를 자동으로 제거 (예: /api/users/ -> /api/users)
        configurer.setUseTrailingSlashMatch(true);
    }
}
