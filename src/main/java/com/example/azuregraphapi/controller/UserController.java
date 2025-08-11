package com.example.azuregraphapi.controller;

import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.dto.GroupDTO;
import com.example.azuregraphapi.service.GraphApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private GraphApiService graphApiService;

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
     * Bonus endpoint: Get all users (requires Directory.Read.All permission)
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
}