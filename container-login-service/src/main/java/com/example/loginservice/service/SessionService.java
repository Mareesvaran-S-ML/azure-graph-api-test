package com.example.loginservice.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service to manage session cookies from the Azure service
 * This stores the session cookies received from the Azure service during login
 * and provides them for subsequent requests
 */
@Service
public class SessionService {
    
    // In-memory storage for session cookies (in production, use Redis or database)
    private final ConcurrentMap<String, String> sessionCookies = new ConcurrentHashMap<>();
    
    /**
     * Store the Azure service session cookie for a user session
     * @param userSessionId The user's session ID (from login service)
     * @param azureSessionCookie The session cookie from Azure service
     */
    public void storeAzureSessionCookie(String userSessionId, String azureSessionCookie) {
        System.out.println("Storing Azure session cookie for user session: " + userSessionId);
        sessionCookies.put(userSessionId, azureSessionCookie);
    }
    
    /**
     * Get the Azure service session cookie for a user session
     * @param userSessionId The user's session ID
     * @return The Azure service session cookie, or null if not found
     */
    public String getAzureSessionCookie(String userSessionId) {
        String cookie = sessionCookies.get(userSessionId);
        if (cookie != null) {
            System.out.println("Retrieved Azure session cookie for user session: " + userSessionId);
        } else {
            System.out.println("No Azure session cookie found for user session: " + userSessionId);
        }
        return cookie;
    }
    
    /**
     * Remove the Azure service session cookie for a user session (on logout)
     * @param userSessionId The user's session ID
     */
    public void removeAzureSessionCookie(String userSessionId) {
        System.out.println("Removing Azure session cookie for user session: " + userSessionId);
        sessionCookies.remove(userSessionId);
    }
    
    /**
     * Extract JSESSIONID from Set-Cookie header
     * @param setCookieHeader The Set-Cookie header value
     * @return The JSESSIONID value, or null if not found
     */
    public String extractJSessionId(String setCookieHeader) {
        if (setCookieHeader == null) {
            return null;
        }
        
        // Look for JSESSIONID in the Set-Cookie header
        if (setCookieHeader.startsWith("JSESSIONID=")) {
            int endIndex = setCookieHeader.indexOf(';');
            if (endIndex > 0) {
                return setCookieHeader.substring(0, endIndex);
            } else {
                return setCookieHeader;
            }
        }
        
        return null;
    }
}
