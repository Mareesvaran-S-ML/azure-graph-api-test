package com.example.loginservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to proxy ALL API calls to azure-entra-auth-test
 *
 * 12-Factor: Backing Services - Treats azure-entra-auth-test as attached resource
 * Acts as a complete proxy/gateway for all azure-entra-auth-test APIs
 */
@Service
public class AzureEntraAuthService {

    private final WebClient webClient;
    private final String azureEntraAuthBaseUrl;
    private final int timeout;
    private final int retryAttempts;

    public AzureEntraAuthService(
            @Value("${AZURE_ENTRA_AUTH_SERVICE_URL:http://container-entra-auth-service:8080}") String azureEntraAuthBaseUrl) {

        this.azureEntraAuthBaseUrl = azureEntraAuthBaseUrl;
        this.timeout = 10000;
        this.retryAttempts = 3;

        this.webClient = WebClient.builder()
                .baseUrl(azureEntraAuthBaseUrl)
                .build();
    }

    /**
     * Get users with pagination from azure-entra-auth-test (without authentication)
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated user response
     */
    public Map<String, Object> getUsers(int page, int size) {
        return getUsersWithAuth(page, size, null);
    }

    /**
     * Get users with pagination from azure-entra-auth-test with authentication
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param headers Authentication headers (including cookies)
     * @return Paginated user response
     */
    public Map<String, Object> getUsersWithAuth(int page, int size, Map<String, String> headers) {
        System.out.println("Getting users with authentication - Page: " + page + ", Size: " + size);

        // First, try the actual users endpoint
        try {
            System.out.println("Trying primary endpoint: /api/graph/users with session authentication...");
            Object response = proxyGet("/api/graph/users", headers);

            if (response != null && isValidUsersResponse(response)) {
                System.out.println("SUCCESS: Got valid users response from /api/graph/users");
                return createPaginatedResponseFromRealData(response, page, size, "/api/graph/users");
            } else {
                System.out.println("No valid users data from /api/graph/users: " + response);
            }

        } catch (Exception e) {
            System.out.println("Failed /api/graph/users: " + e.getMessage());
        }

        // If users endpoint fails, try fallback endpoints for any user data
        String[] fallbackEndpoints = {
            "/api/graph/user/profile", // Single user profile
            "/api/graph/groups",       // Groups (can be converted to user-like data)
            "/api/graph/roles"         // Roles (can be converted to user-like data)
        };

        for (String endpoint : fallbackEndpoints) {
            try {
                System.out.println("Trying fallback endpoint: " + endpoint + " with session authentication...");
                Object response = proxyGet(endpoint, headers);

                if (response != null && isValidResponse(response)) {
                    System.out.println("SUCCESS: Got valid response from fallback " + endpoint);
                    return createPaginatedResponseFromRealData(response, page, size, endpoint);
                } else {
                    System.out.println("No valid data from " + endpoint + ": " + response);
                }

            } catch (Exception e) {
                System.out.println("Failed " + endpoint + ": " + e.getMessage());
            }
        }

        // If all endpoints fail, return error
        System.out.println("All endpoints failed, azure-entra-auth-test service unavailable");
        return createErrorResponse("Service Unavailable",
            "Cannot retrieve users - azure-entra-auth-test service is not available");
    }

    /**
     * Check if response is specifically a valid users response
     */
    private boolean isValidUsersResponse(Object response) {
        if (response instanceof Map) {
            Map<?, ?> responseMap = (Map<?, ?>) response;

            // Check if it's an explicit error response
            if (responseMap.containsKey("error") && responseMap.get("error") != null) {
                return false;
            }

            // Valid users response should have success=true and data array
            boolean hasSuccess = responseMap.containsKey("success") && Boolean.TRUE.equals(responseMap.get("success"));
            boolean hasData = responseMap.containsKey("data") && responseMap.get("data") instanceof List;

            System.out.println("isValidUsersResponse: hasSuccess=" + hasSuccess + ", hasData=" + hasData);
            return hasSuccess && hasData;
        }
        return false;
    }

    /**
     * Check if response is valid (not an error response)
     */
    private boolean isValidResponse(Object response) {
        if (response instanceof Map) {
            Map<?, ?> responseMap = (Map<?, ?>) response;

            // Check if it's an explicit error response
            if (responseMap.containsKey("error") && responseMap.get("error") != null) {
                return false;
            }

            // PRIORITY 1: Azure Graph API format with success=true and data array (for users)
            if (responseMap.containsKey("success") && Boolean.TRUE.equals(responseMap.get("success")) &&
                responseMap.containsKey("data") && responseMap.get("data") instanceof List) {
                System.out.println("✅ Valid Azure Graph API response with success=true and data array");
                return true;
            }

            // PRIORITY 2: Other valid response formats
            boolean isValid = responseMap.containsKey("users") ||
                   responseMap.containsKey("value") ||
                   responseMap.containsKey("displayName") ||
                   responseMap.containsKey("mail") ||
                   responseMap.containsKey("userPrincipalName") ||
                   (responseMap.containsKey("status") && "UP".equals(responseMap.get("status"))) ||
                   responseMap.containsKey("service");

            if (isValid) {
                System.out.println("✅ Valid response with standard format");
            } else {
                System.out.println("❌ Invalid response format: " + responseMap.keySet());
            }
            return isValid;
        }
        return response instanceof List && !((List<?>) response).isEmpty();
    }





    /**
     * Create paginated response from real data obtained from azure-entra-auth-test
     */
    private Map<String, Object> createPaginatedResponseFromRealData(Object usersResponse, int page, int size, String sourceEndpoint) {
        Map<String, Object> response = new HashMap<>();

        // Handle different response types
        List<Map<String, Object>> allUsers = new ArrayList<>();

        if (usersResponse instanceof Map) {
            Map<String, Object> usersData = (Map<String, Object>) usersResponse;

            // If it's a single user (like from /user/profile or /me)
            if (usersData.containsKey("displayName") || usersData.containsKey("mail")) {
                allUsers.add(usersData);
            } else if (usersData.containsKey("data")) {
                // Handle Azure Graph API response format: {success: true, data: [...], count: N}
                Object dataObj = usersData.get("data");
                if (dataObj instanceof List) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;

                    if (sourceEndpoint.equals("/api/graph/users")) {
                        // For users endpoint, data contains UserDTO objects - use them directly
                        System.out.println("📊 Processing users data: found " + dataList.size() + " users");
                        allUsers = dataList;
                    } else {
                        // For groups/roles endpoints, convert to user-like format for pagination
                        for (Map<String, Object> item : dataList) {
                            Map<String, Object> userLikeItem = new HashMap<>();
                            userLikeItem.put("id", item.get("id"));
                            userLikeItem.put("displayName", item.get("displayName"));
                            userLikeItem.put("description", item.get("description"));
                            userLikeItem.put("type", sourceEndpoint.contains("groups") ? "group" : "role");
                            allUsers.add(userLikeItem);
                        }
                    }
                }
            } else {
                // Extract users list from response (legacy format)
                allUsers = (List<Map<String, Object>>) usersData.getOrDefault("users",
                    usersData.getOrDefault("value", List.of()));
            }
        } else if (usersResponse instanceof List) {
            allUsers = (List<Map<String, Object>>) usersResponse;
        }

        // Calculate pagination
        int totalElements = allUsers.size();
        int totalPages = (totalElements + size - 1) / size;
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        System.out.println("📄 Pagination: totalElements=" + totalElements + ", page=" + page + ", size=" + size);
        System.out.println("📄 Pagination: startIndex=" + startIndex + ", endIndex=" + endIndex + ", totalPages=" + totalPages);

        // Get page subset
        List<Map<String, Object>> pageUsers = allUsers.subList(
            Math.min(startIndex, totalElements),
            Math.min(endIndex, totalElements)
        );

        System.out.println("📄 Returning " + pageUsers.size() + " users for page " + page);

        response.put("users", pageUsers);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("source", "azure-entra-auth-test");
        response.put("sourceEndpoint", sourceEndpoint);
        response.put("message", "Real data from azure-entra-auth-test service");
        response.put("serviceUrl", azureEntraAuthBaseUrl);
        response.put("connectionTest", testConnection());

        return response;
    }

    private Map<String, Object> createPaginatedResponse(Object usersResponse, int page, int size) {
        return createPaginatedResponseFromRealData(usersResponse, page, size, "legacy");
    }



    public Map<String, Object> testConnection() {
        Map<String, Object> test = new HashMap<>();
        test.put("targetUrl", azureEntraAuthBaseUrl);

        try {
            Map<String, Object> healthResponse = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            test.put("status", "SUCCESS");
            test.put("response", healthResponse);

        } catch (WebClientResponseException e) {
            test.put("status", "HTTP_ERROR");
            test.put("statusCode", e.getStatusCode().value());
            test.put("error", e.getMessage());

        } catch (Exception e) {
            test.put("status", "CONNECTION_FAILED");
            test.put("error", e.getMessage());
        }

        return test;
    }

    /**
     * Proxy POST request to azure-entra-auth-test
     */
    public Object proxyPost(String path, Object requestBody, Map<String, String> headers) {
        try {
            System.out.println("Proxying POST request to: " + path);
            if (headers != null && headers.containsKey("Cookie")) {
                System.out.println("Forwarding Cookie: " + headers.get("Cookie"));
            }

            var requestSpec = webClient.post().uri(path);

            // Add headers if provided
            if (headers != null) {
                headers.forEach((key, value) -> {
                    System.out.println("Adding header: " + key + " = " + value);
                    requestSpec.header(key, value);
                });
            }

            // Add request body if provided
            if (requestBody != null) {
                requestSpec.bodyValue(requestBody);
            }

            return requestSpec
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

        } catch (Exception e) {
            System.out.println("Error proxying POST to " + path + ": " + e.getMessage());
            return createErrorResponse("Failed to proxy POST request", e.getMessage());
        }
    }

    /**
     * Proxy GET request to azure-entra-auth-test
     */
    public Object proxyGet(String path, Map<String, String> headers) {
        try {
            System.out.println("Proxying GET request to: " + path);
            if (headers != null && headers.containsKey("Cookie")) {
                System.out.println("Forwarding Cookie: " + headers.get("Cookie"));
            }

            var requestSpec = webClient.get().uri(path);

            // Add headers if provided
            if (headers != null) {
                headers.forEach((key, value) -> {
                    System.out.println("Adding header: " + key + " = " + value);
                    requestSpec.header(key, value);
                });
            }

            return requestSpec
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

        } catch (Exception e) {
            System.out.println("Error proxying GET to " + path + ": " + e.getMessage());
            return createErrorResponse("Failed to proxy GET request", e.getMessage());
        }
    }

    /**
     * Proxy any HTTP method to azure-entra-auth-test
     */
    public Object proxyRequest(String method, String path, Object requestBody, Map<String, String> headers) {
        if ("POST".equalsIgnoreCase(method)) {
            return proxyPost(path, requestBody, headers);
        } else if ("GET".equalsIgnoreCase(method)) {
            return proxyGet(path, headers);
        } else {
            return createErrorResponse("Unsupported HTTP method", "Method " + method + " not supported");
        }
    }

    private Map<String, Object> createErrorResponse(String message, String details) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("details", details);
        error.put("timestamp", System.currentTimeMillis());
        error.put("service", "container-login-service");
        return error;
    }
}
