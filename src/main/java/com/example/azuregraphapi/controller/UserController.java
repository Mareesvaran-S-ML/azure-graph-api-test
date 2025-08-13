package com.example.azuregraphapi.controller;

import com.example.azuregraphapi.dto.GroupDTO;
import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.service.GraphApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    public ResponseEntity<UserDTO> getCurrentUserProfile(Authentication authentication, HttpServletRequest request) {
        try {
            UserDTO user = graphApiService.getCurrentUser(authentication, request);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }



    /**
     * Get all users (requires Directory.Read.All permission)
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(Authentication authentication, HttpServletRequest request) {
        try {
            List<UserDTO> users = graphApiService.getAllUsers(authentication, request);
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
     * Direct login with username and password
     * This endpoint accepts username/password and authenticates directly with Azure AD
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> loginWithCredentials(@RequestBody Map<String, String> credentials,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {
        System.out.println("POST /api/auth/login called with credentials: " + credentials.keySet());
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            System.out.println("Username: " + username + ", Password length: " + (password != null ? password.length() : 0));

            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("authenticated", false);
                errorResponse.put("error", "Username and password are required");
                return ResponseEntity.status(400).body(errorResponse);
            }

            // Authenticate with Azure AD using username/password
            Map<String, Object> authResult = graphApiService.authenticateWithCredentials(username, password);

            if ((Boolean) authResult.get("authenticated")) {
                // Create session
                String sessionId = request.getSession().getId();

                // Store authentication info in session
                request.getSession().setAttribute("azure_access_token", authResult.get("access_token"));
                request.getSession().setAttribute("azure_user_id", authResult.get("user_id"));
                request.getSession().setAttribute("authenticated", true);

                // Create a Spring Security authentication token
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        authResult.get("user_id"), null, java.util.Collections.emptyList());
                authToken.setDetails(authResult);
                SecurityContextHolder.getContext().setAuthentication(authToken);

                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", true);
                result.put("session_code", sessionId);
                result.put("user_id", authResult.get("user_id"));
                result.put("expires_at", authResult.get("expires_at"));
                result.put("login_time", System.currentTimeMillis());
                result.put("message", "Use Cookie: JSESSIONID=" + sessionId + " header in subsequent API calls");

                response.setContentType("application/json");
                return ResponseEntity.ok(result);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("authenticated", false);
                errorResponse.put("error", authResult.get("error"));
                return ResponseEntity.status(401).body(errorResponse);
            }

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("authenticated", false);
            errorResponse.put("error", "Authentication failed: " + e.getMessage());

            response.setContentType("application/json");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Logout endpoint - invalidates session and returns JSON response
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Get current session
            HttpSession session = request.getSession(false);

            Map<String, Object> result = new HashMap<>();

            if (session != null) {
                // Clear session attributes
                session.removeAttribute("azure_access_token");
                session.removeAttribute("azure_user_id");
                session.removeAttribute("authenticated");

                // Invalidate session
                session.invalidate();

                result.put("success", true);
                result.put("message", "Successfully logged out");
            } else {
                result.put("success", true);
                result.put("message", "No active session found");
            }

            // Clear Spring Security context
            SecurityContextHolder.clearContext();

            result.put("timestamp", System.currentTimeMillis());

            response.setContentType("application/json");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Logout failed: " + e.getMessage());

            response.setContentType("application/json");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    @GetMapping("/roles")
    @ResponseBody
    public ResponseEntity<List<String>> getAllCustomRoles(Authentication authentication, HttpServletRequest request) {
        try {
            List<String> roles = graphApiService.getAllCustomRoles(authentication, request);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // All Security Groups API (returns JSON)
    @GetMapping("/groups")
    @ResponseBody
    public ResponseEntity<List<GroupDTO>> getAllSecurityGroups(Authentication authentication, HttpServletRequest request) {
        try {
            List<GroupDTO> groups = graphApiService.getAllSecurityGroups(authentication, request);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}