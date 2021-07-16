package com.ftseoul.visitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowedMethods("GET", "OPTIONS", "POST", "HEAD", "DELETE", "PUT", "PATCH")
                .maxAge(3600)
                .allowCredentials(true)
                .allowedOrigins("http://localhost:3000", "https://visitor.dev.42seoul.io", "http://visitor.dev.42seoul.io");
    }
}
