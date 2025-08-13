package com.example.azuregraphapi.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Skip authentication for login and logout endpoints
        if (request.getRequestURI().equals("/api/auth/login") ||
            request.getRequestURI().equals("/api/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if user is already authenticated via OAuth2
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated() && 
            !existingAuth.getName().equals("anonymousUser")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check for session-based authentication
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object authenticated = session.getAttribute("authenticated");
            Object userId = session.getAttribute("azure_user_id");
            Object accessToken = session.getAttribute("azure_access_token");
            
            if (Boolean.TRUE.equals(authenticated) && userId != null && accessToken != null) {
                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                authToken.setDetails(session.getAttribute("azure_access_token"));
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
