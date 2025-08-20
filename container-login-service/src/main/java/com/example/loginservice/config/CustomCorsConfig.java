package com.example.loginservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Custom CORS Configuration for Container Login Service
 * 
 * Configures CORS with credentials support for cookie-based authentication
 * and proper cross-origin communication with frontend applications.
 */
@Configuration
public class CustomCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // === Allowed Origins ===
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",           // React/Vue/Angular dev server
            "http://localhost:8080",           // Local backend
            "http://localhost:8087",           // Azure Graph API service
            "http://localhost:8089",           // Container Login service
            "https://app.yourdomain.com"       // Production frontend (update as needed)
        ));

        // === Allowed Methods ===
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // === Allow Credentials (CRITICAL for cookies and sessions) ===
        config.setAllowCredentials(true);

        // === Allowed Headers ===
        config.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Cookie",
            "Set-Cookie"
        ));

        // === Exposed Headers (so client can read them) ===
        config.setExposedHeaders(Arrays.asList(
            "Set-Cookie",
            "Authorization"
        ));

        // === Cache preflight requests for 1 hour ===
        config.setMaxAge(3600L);

        // === Apply config to all paths ===
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
