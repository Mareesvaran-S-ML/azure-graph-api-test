package com.example.loginservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for Container Login Service
 *
 * Uses CorsConfigurationSource approach for reliable CORS handling
 * Allows localhost origins but blocks 127.0.0.1 for security
 */
@Configuration
public class CustomCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // === Allowed Origins ===
        // localhost is allowed, but 127.0.0.1 is NOT allowed
        List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3000",           // React/Vue/Angular dev server
            "http://localhost:8080",           // Local backend
            "http://localhost:8087",           // Azure Graph API service
            "http://localhost:8089",           // Container Login service
            "https://app.yourdomain.com",      // Production frontend
            "null"                             // For file:// protocol testing
            // 127.0.0.1 addresses are intentionally excluded for security
        );

        configuration.setAllowedOrigins(allowedOrigins);

        // === Allowed Methods ===
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // === Allowed Headers ===
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // === Allow Credentials (CRITICAL for cookies and sessions) ===
        configuration.setAllowCredentials(true);

        // === Exposed Headers ===
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));

        // === Cache preflight requests for 1 hour ===
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
