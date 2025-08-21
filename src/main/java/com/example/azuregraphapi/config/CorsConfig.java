package com.example.azuregraphapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for Azure Graph API Service
 * 
 * Configures Cross-Origin Resource Sharing with proper cookie support
 * for session-based authentication across different origins.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${api.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:8087,http://localhost:8089}")
    private String allowedOrigins;

    @Value("${api.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${api.cors.allowed-headers:*}")
    private String allowedHeaders;

    /**
     * Global CORS configuration for all endpoints
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        List<String> methods = Arrays.asList(allowedMethods.split(","));

        // NOTE: Only localhost origins are allowed, 127.0.0.1 is blocked for security
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods(methods.toArray(new String[0]))
                .allowedHeaders("*")
                .allowCredentials(true)  // ✅ Enable cookies/credentials
                .maxAge(3600); // Cache preflight for 1 hour
    }

    /**
     * CORS Configuration Source for Spring Security
     * This is used by Spring Security's CORS filter
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from configuration
        // NOTE: Only localhost origins are allowed, 127.0.0.1 is blocked for security
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Parse allowed methods from configuration
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // ✅ CRITICAL: Enable credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose common headers to the client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Set-Cookie"  // ✅ Expose Set-Cookie header
        ));
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
