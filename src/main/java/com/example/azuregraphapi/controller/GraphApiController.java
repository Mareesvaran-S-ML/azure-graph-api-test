package com.example.azuregraphapi.controller;

import com.example.azuregraphapi.dto.GroupDTO;
import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.service.GraphApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated controller for Azure Graph API operations
 * All endpoints include /graph prefix for clear API organization
 * Designed for backend service consumption
 */
@RestController
@RequestMapping("/api/graph")
public class GraphApiController {

    @Autowired
    private GraphApiService graphApiService;

    // ========================================
    // USER OPERATIONS
    // ========================================

    /**
     * Get current authenticated user's profile from Azure Graph API
     * 
     * @param authentication Spring Security authentication object
     * @param request HTTP request for session management
     * @return UserDTO with user profile information
     */
    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile(
            Authentication authentication, 
            HttpServletRequest request) {
        try {
            UserDTO user = graphApiService.getCurrentUser(authentication, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", user);
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "azure_graph_api");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve user profile");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("source", "azure_graph_api");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get all users from Azure Graph API
     * Requires Directory.Read.All permission
     * 
     * @param authentication Spring Security authentication object
     * @param request HTTP request for session management
     * @return List of UserDTO objects
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            Authentication authentication, 
            HttpServletRequest request) {
        try {
            List<UserDTO> users = graphApiService.getAllUsers(authentication, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", users);
            response.put("count", users.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "azure_graph_api");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve users");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("source", "azure_graph_api");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========================================
    // ROLE OPERATIONS
    // ========================================

    /**
     * Get all custom roles from Azure Graph API
     * 
     * @param authentication Spring Security authentication object
     * @param request HTTP request for session management
     * @return List of role names
     */
    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getAllCustomRoles(
            Authentication authentication, 
            HttpServletRequest request) {
        try {
            List<String> roles = graphApiService.getAllCustomRoles(authentication, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", roles);
            response.put("count", roles.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "azure_graph_api");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve roles");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("source", "azure_graph_api");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========================================
    // GROUP OPERATIONS
    // ========================================

    /**
     * Get all security groups from Azure Graph API
     * 
     * @param authentication Spring Security authentication object
     * @param request HTTP request for session management
     * @return List of GroupDTO objects
     */
    @GetMapping("/groups")
    public ResponseEntity<Map<String, Object>> getAllSecurityGroups(
            Authentication authentication, 
            HttpServletRequest request) {
        try {
            List<GroupDTO> groups = graphApiService.getAllSecurityGroups(authentication, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", groups);
            response.put("count", groups.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "azure_graph_api");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve groups");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("source", "azure_graph_api");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========================================
    // UTILITY ENDPOINTS
    // ========================================

    /**
     * Get Graph API service status and capabilities
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getGraphApiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "azure_graph_api");
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        status.put("version", "1.0.0");
        status.put("capabilities", Map.of(
            "user_profile", "Get current user profile",
            "all_users", "Get all users (requires Directory.Read.All)",
            "custom_roles", "Get custom roles",
            "security_groups", "Get security groups"
        ));
        status.put("endpoints", Map.of(
            "user_profile", "/api/graph/user/profile",
            "all_users", "/api/graph/users",
            "roles", "/api/graph/roles",
            "groups", "/api/graph/groups"
        ));
        
        return ResponseEntity.ok(status);
    }
}
