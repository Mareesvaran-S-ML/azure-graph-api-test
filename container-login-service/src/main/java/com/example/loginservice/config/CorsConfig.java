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
 * Allows specific origins for frontend applications to access the login service
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // === Allowed Origins ===
        // Only allow specific localhost origins
        List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3000",           // React/Vue/Angular dev server
            "http://localhost:8089",           // Container Login service (self)
            "http://localhost:8087"            // Azure Graph API service
        );

        configuration.setAllowedOriginPatterns(allowedOrigins);

        // === Allowed Methods ===
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // === Allowed Headers ===
        // Specific headers when allowCredentials is true
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Cookie",
            "X-Session-Code"
        ));

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
