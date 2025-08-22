package com.example.loginservice.controller;

import com.example.loginservice.model.ProxyResponse;
import com.example.loginservice.service.AzureEntraAuthService;
import com.example.loginservice.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * REST Controller for Container Login Service
 *
 * Provides ALL the same APIs as azure-entra-auth-test with pagination support
 */
@RestController
public class UserController {

    @Autowired
    private AzureEntraAuthService azureEntraAuthService;

    @Autowired
    private SessionService sessionService;

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    /**
     * Home endpoint - Service status
     */
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "container-login-service");
        response.put("status", "UP");
        response.put("message", "Login service with pagination - communicates with azure-entra-auth-test");
        response.put("version", "1.0.0");
        response.put("integration", azureEntraAuthService.testConnection());
        return response;
    }

    /**
     * Users endpoint with pagination
     *
     * This is the main endpoint that provides the same data as azure-entra-auth-test /users
     * but with pagination support
     *
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Paginated users response
     */
    @GetMapping({"/users", "/web/users"})
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = defaultPageSize;
        } else if (size > maxPageSize) {
            size = maxPageSize;
        }

        // Extract authentication headers
        Map<String, String> headers = extractHeaders(request);

        // Get users from azure-entra-auth-test with authentication and pagination
        return azureEntraAuthService.getUsersWithAuth(page, size, headers);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health-check")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "container-login-service");
        response.put("timestamp", System.currentTimeMillis());
        response.put("azureEntraAuthConnection", azureEntraAuthService.testConnection());
        return response;
    }

    /**
     * Connection test endpoint
     */
    @GetMapping("/test-connection")
    public Map<String, Object> testConnection() {
        return azureEntraAuthService.testConnection();
    }

    // ========================================
    // PROXY ALL AZURE-ENTRA-AUTH-TEST APIs
    // ========================================

    /**
     * Authentication Login - Web API endpoint that proxies to azure-entra-auth-test
     * Now properly captures and forwards session cookies from Azure service
     */
    @PostMapping("/web/auth/login")
    public ResponseEntity<?> login(@RequestBody Object loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            // Proxy login request to Azure Graph API service and capture response headers
            ProxyResponse proxyResponse = azureEntraAuthService.proxyPostWithHeaders("/api/auth/login", loginRequest, null);

            // Check if login was successful
            if (proxyResponse.getBody() instanceof Map) {
                Map<?, ?> responseMap = (Map<?, ?>) proxyResponse.getBody();

                // If login successful, capture and store the Azure service session cookie
                if (responseMap.containsKey("authenticated") && Boolean.TRUE.equals(responseMap.get("authenticated"))) {

                    // Get the current user's session ID (create one if needed)
                    String userSessionId = request.getSession().getId();

                    // Extract Azure service session cookie from response headers
                    List<String> setCookieHeaders = proxyResponse.getHeaders().get("Set-Cookie");
                    if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                        for (String setCookieHeader : setCookieHeaders) {
                            String azureSessionCookie = sessionService.extractJSessionId(setCookieHeader);
                            if (azureSessionCookie != null) {
                                // Store the Azure service session cookie
                                sessionService.storeAzureSessionCookie(userSessionId, azureSessionCookie);
                                System.out.println("✅ Captured and stored Azure session cookie: " + azureSessionCookie);
                                break;
                            }
                        }
                    }

                    // Create our own session cookie for the frontend
                    ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", userSessionId)
                            .httpOnly(true)
                            .secure(false)  // For development - set to true in production
                            .sameSite("Strict")
                            .maxAge(Duration.ofHours(1))
                            .path("/")
                            .build();

                    response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());
                    System.out.println("✅ Session cookie set for successful login");
                }
            }

            return ResponseEntity.ok(proxyResponse.getBody());

        } catch (Exception e) {
            System.out.println("❌ Login error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Login failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Generate a simple session ID (in production, use proper session management)
     */
    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * Authentication Status - Web API endpoint that proxies to azure-entra-auth-test
     */
    @GetMapping("/web/auth/status")
    public Object authStatus(HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        return azureEntraAuthService.proxyGet("/api/auth/status", headers);
    }

    /**
     * Get Current User - Web API endpoint that proxies to azure-entra-auth-test
     */
    @GetMapping("/web/auth/user")
    public Object getCurrentUser(HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        return azureEntraAuthService.proxyGet("/api/auth/user", headers);
    }

    /**
     * Logout - Web API endpoint that proxies to azure-entra-auth-test
     * Now properly clears cookies in the response
     */
    @PostMapping("/web/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Get user session ID before logout
            String userSessionId = request.getSession().getId();

            Map<String, String> headers = extractHeaders(request);
            Object logoutResponse = azureEntraAuthService.proxyPost("/api/auth/logout", null, headers);

            // Remove stored Azure session cookie
            sessionService.removeAzureSessionCookie(userSessionId);

            // Clear session cookie on logout
            ResponseCookie clearCookie = ResponseCookie.from("JSESSIONID", "")
                    .httpOnly(true)
                    .secure(false)  // For development - set to true in production
                    .sameSite("Strict")  // Don't use "None" for security
                    .maxAge(0)  // Expire immediately
                    .path("/")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            System.out.println("✅ Session cookie cleared on logout");

            return ResponseEntity.ok(logoutResponse);

        } catch (Exception e) {
            System.out.println("❌ Logout error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get Roles - Web API endpoint that proxies to azure-entra-auth-test Graph API
     */
    @GetMapping("/web/roles")
    public Object getRoles(HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        return azureEntraAuthService.proxyGet("/api/graph/roles", headers);
    }

    /**
     * Get Groups - Web API endpoint that proxies to azure-entra-auth-test Graph API
     */
    @GetMapping("/web/groups")
    public Object getGroups(HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        return azureEntraAuthService.proxyGet("/api/graph/groups", headers);
    }

    /**
     * Health endpoint - Proxy to azure-entra-auth-test
     */
    @GetMapping("/health")
    public Object health() {
        return azureEntraAuthService.proxyGet("/health", null);
    }

    /**
     * API v1 Users (original without pagination) - Proxy to azure-entra-auth-test
     */
    @GetMapping("/api/v1/users")
    public Object getApiV1Users(HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        return azureEntraAuthService.proxyGet("/api/v1/users", headers);
    }

    /**
     * User Profile - Web API endpoint that proxies to azure-entra-auth-test Graph API
     * Should return current user data only, not all users
     */
    @GetMapping("/web/user/profile")
    public Object getUserProfile(HttpServletRequest request) {
        Map<String, String> headers = extractHeaders(request);
        return azureEntraAuthService.proxyGet("/api/graph/user/profile", headers);
    }





    /**
     * Extract important headers from request for proxying
     * Now uses the stored Azure service session cookie instead of the frontend cookie
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();

        // Get the user's session ID
        String userSessionId = request.getSession().getId();

        // Get the stored Azure service session cookie
        String azureSessionCookie = sessionService.getAzureSessionCookie(userSessionId);
        if (azureSessionCookie != null) {
            headers.put("Cookie", azureSessionCookie);
            System.out.println("Using stored Azure session cookie: " + azureSessionCookie);
        } else {
            System.out.println("No Azure session cookie found for user session: " + userSessionId);
            // Fallback to original cookie if available
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null) {
                headers.put("Cookie", cookieHeader);
                System.out.println("Fallback to original cookie: " + cookieHeader);
            }
        }

        // Forward Authorization header if present
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.put("Authorization", authHeader);
        }

        return headers;
    }

    /**
     * Generic proxy for web endpoints - maps /web/* to appropriate backend endpoints
     */
    @RequestMapping(value = "/web/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public Object proxyWebRequests(
            HttpServletRequest request,
            @RequestBody(required = false) Object requestBody) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Map /web/* requests to appropriate backend endpoints
        String backendPath = mapWebPathToBackendPath(path);

        // Extract and forward important headers including cookies
        Map<String, String> headers = new HashMap<>();

        // Forward Cookie header for session management
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            headers.put("Cookie", cookieHeader);
        }

        // Forward Authorization header if present
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.put("Authorization", authHeader);
        }

        // Forward Content-Type for POST/PUT requests
        String contentType = request.getHeader("Content-Type");
        if (contentType != null) {
            headers.put("Content-Type", contentType);
        }

        return azureEntraAuthService.proxyRequest(method, backendPath, requestBody, headers);
    }

    /**
     * Map /web/* paths to appropriate backend API paths
     */
    private String mapWebPathToBackendPath(String webPath) {
        // Remove /web prefix and map to appropriate backend endpoints
        if (webPath.startsWith("/web/auth/")) {
            // /web/auth/* -> /api/auth/*
            return webPath.replace("/web/auth/", "/api/auth/");
        } else if (webPath.startsWith("/web/")) {
            // /web/* -> /api/graph/* (for users, roles, groups, etc.)
            String remaining = webPath.substring("/web/".length());
            return "/api/graph/" + remaining;
        }

        // Default: return as-is
        return webPath;
    }
}
