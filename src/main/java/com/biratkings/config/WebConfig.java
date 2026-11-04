package com.biratkings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    /** Pattern-based so phones on the LAN (http://192.168.x.x:port) and localhost work out of the box. */
    @Bean
    WebMvcConfigurer cors(@Value("${app.cors.origin-patterns}") String[] patterns) {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry r) {
                r.addMapping("/api/**").allowedOriginPatterns(patterns)
                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*").maxAge(3600);
            }
        };
    }
}
