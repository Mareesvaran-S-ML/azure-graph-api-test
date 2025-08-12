package com.example.azuregraphapi.controller;

import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.dto.GroupDTO;
import com.example.azuregraphapi.service.GraphApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private GraphApiService graphApiService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Endpoint 1: Get current authenticated user's details including roles and groups
     */
    @GetMapping("/user/profile")
    public ResponseEntity<UserDTO> getCurrentUserProfile(Authentication authentication) {
        try {
            UserDTO user = graphApiService.getCurrentUser(authentication);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Endpoint 2: Get current user's security groups
     */
    @GetMapping("/user/groups")
    public ResponseEntity<List<GroupDTO>> getCurrentUserGroups(Authentication authentication) {
        try {
            List<GroupDTO> groups = graphApiService.getUserGroups(authentication, null);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Endpoint 3: Get current user's roles
     */
    @GetMapping("/user/roles")
    public ResponseEntity<List<String>> getCurrentUserRoles(Authentication authentication) {
        try {
            List<String> roles = graphApiService.getUserRoles(authentication, null);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get all users (requires Directory.Read.All permission)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(Authentication authentication) {
        try {
            List<UserDTO> users = graphApiService.getAllUsers(authentication);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get access token for Postman testing
     * This endpoint helps with Postman OAuth2 testing
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getTokenInfo(Authentication authentication) {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("access_token", client.getAccessToken().getTokenValue());
            tokenInfo.put("token_type", "Bearer");
            tokenInfo.put("expires_at", client.getAccessToken().getExpiresAt());
            tokenInfo.put("scopes", client.getAccessToken().getScopes());

            return ResponseEntity.ok(tokenInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get token: " + e.getMessage()));
        }
    }

    /**
     * Get session code after login - for UI team authentication
     * This endpoint returns a session code that can be used for subsequent API calls
     */
    @GetMapping("/auth/session")
    public ResponseEntity<Map<String, Object>> getSessionCode(Authentication authentication,
                                                              HttpServletRequest request) {
        try {
            // Get session ID
            String sessionId = request.getSession().getId();

            // Get user info
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String userId = oauthToken.getName();

            // Get access token info
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("session_code", sessionId);
            sessionInfo.put("user_id", userId);
            sessionInfo.put("authenticated", true);
            sessionInfo.put("expires_at", client.getAccessToken().getExpiresAt());
            sessionInfo.put("login_time", System.currentTimeMillis());
            sessionInfo.put("message", "Use this session_code as 'X-Session-Code' header in subsequent API calls");

            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get session code: " + e.getMessage()));
        }
    }

    /**
     * Complete OAuth2 login and return session code directly
     * This endpoint handles the complete flow:
     * 1. If not authenticated: redirects to Azure AD
     * 2. If authenticated: returns session code as JSON
     */
    @GetMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> loginAndGetSessionCode(Authentication authentication,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) throws IOException {
        try {
            // Check if user is already authenticated
            if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication instanceof OAuth2AuthenticationToken)) {
                // Not authenticated, redirect to Azure AD
                response.sendRedirect("/oauth2/authorization/azure");
                return null; // Response will be handled by redirect
            }

            // User is authenticated, return session code as JSON
            String sessionId = request.getSession().getId();

            // Get user info
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String userId = oauthToken.getName();

            // Get access token info
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("authenticated", true);
            result.put("session_code", sessionId);
            result.put("user_id", userId);
            result.put("expires_at", client.getAccessToken().getExpiresAt());
            result.put("login_time", System.currentTimeMillis());
            result.put("message", "Use Cookie: JSESSIONID=" + sessionId + " header in subsequent API calls");

            // Set content type to ensure JSON response
            response.setContentType("application/json");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("authenticated", false);
            errorResponse.put("error", "Failed to process authentication: " + e.getMessage());

            response.setContentType("application/json");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

}