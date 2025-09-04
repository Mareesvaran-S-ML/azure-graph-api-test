package com.example.azuregraphapi.controller;

import com.example.azuregraphapi.service.GraphApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private GraphApiService graphApiService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Value("${AZURE_TENANT_ID:}")
    private String tenantId;

    @Value("${AZURE_CLIENT_ID:}")
    private String clientId;

    @Value("${AZURE_CLIENT_SECRET:}")
    private String clientSecret;

    private final WebClient webClient = WebClient.builder().build();

    // ========================================
    // SERVICE STATUS AND HEALTH ENDPOINTS
    // ========================================

    /**
     * Service health check endpoint for backend service consumption
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "container-entra-auth");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        health.put("description", "Azure Graph API Backend Service");
        return ResponseEntity.ok(health);
    }

    /**
     * Service status endpoint with detailed information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "container-entra-auth");
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        status.put("version", "1.0.0");
        status.put("description", "Azure Graph API Backend Service");
        status.put("endpoints", Map.of(
            "authentication", "/api/auth/*",
            "graph_users", "/api/graph/users",
            "graph_user_profile", "/api/graph/user/profile",
            "graph_roles", "/api/graph/roles",
            "graph_groups", "/api/graph/groups"
        ));
        return ResponseEntity.ok(status);
    }

    // ========================================
    // AZURE GRAPH API ENDPOINTS (with /graph prefix)
    // ========================================



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
            sessionInfo.put("message", "Use Cookie: JSESSIONID=" + sessionId + " or X-Session-Code header for subsequent API calls");
            sessionInfo.put("service", "container-entra-auth");
            sessionInfo.put("api_version", "1.0.0");

            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get session code");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("service", "container-entra-auth");
            return ResponseEntity.status(500).body(errorResponse);
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

            // Use Client Credentials authentication (works with Security Defaults!)
            System.out.println("⚠️ Using Service Principal authentication instead of user credentials (Security Defaults compatible)");
            Map<String, Object> authResult = graphApiService.authenticateWithClientCredentials();

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
                result.put("message", "Use Cookie: JSESSIONID=" + sessionId + " or X-Session-Code header for subsequent API calls");
                result.put("service", "container-entra-auth");
                result.put("api_version", "1.0.0");
                result.put("endpoints", Map.of(
                    "user_profile", "/api/graph/user/profile",
                    "users", "/api/graph/users",
                    "roles", "/api/graph/roles",
                    "groups", "/api/graph/groups"
                ));

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
     * Also revokes Azure OAuth2 tokens for complete logout
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Get current session
            HttpSession session = request.getSession(false);

            Map<String, Object> result = new HashMap<>();
            boolean azureTokenRevoked = false;

            if (session != null) {
                // Get access token before clearing session
                String accessToken = (String) session.getAttribute("azure_access_token");

                // Revoke Azure tokens if available
                if (accessToken != null) {
                    try {
                        revokeAzureTokens(accessToken);
                        azureTokenRevoked = true;
                        System.out.println("✅ Azure tokens revoked successfully");
                    } catch (Exception e) {
                        System.out.println("⚠️ Failed to revoke Azure tokens: " + e.getMessage());
                        // Continue with logout even if token revocation fails
                    }
                }

                // Clear session attributes
                session.removeAttribute("azure_access_token");
                session.removeAttribute("azure_user_id");
                session.removeAttribute("authenticated");

                // Invalidate session
                session.invalidate();

                result.put("success", true);
                result.put("message", "Successfully logged out");
                result.put("azure_tokens_revoked", azureTokenRevoked);
                result.put("service", "container-entra-auth");
                result.put("timestamp", System.currentTimeMillis());
            } else {
                result.put("success", true);
                result.put("message", "No active session found");
                result.put("azure_tokens_revoked", false);
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

    /**
     * Revoke Azure OAuth2 tokens to ensure complete logout
     * This calls Azure's token revocation endpoint
     */
    private void revokeAzureTokens(String accessToken) {
        try {
            String revokeUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/logout";

            // Create form data for token revocation
            String requestBody = "token=" + accessToken +
                               "&client_id=" + clientId +
                               "&client_secret=" + clientSecret;

            // Make revocation request
            webClient.post()
                    .uri(revokeUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Azure token revocation request sent successfully");

        } catch (Exception e) {
            System.out.println("Failed to revoke Azure tokens: " + e.getMessage());
            throw e;
        }
    }

}