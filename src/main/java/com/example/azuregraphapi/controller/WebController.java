package com.example.azuregraphapi.controller;

import com.example.azuregraphapi.dto.GroupDTO;
import com.example.azuregraphapi.dto.RoleDTO;
import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.service.GraphApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private GraphApiService graphApiService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        try {
            UserDTO user = graphApiService.getCurrentUser(authentication);
            model.addAttribute("user", user);
            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load user information: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/users")
    public String users(Authentication authentication, Model model) {
        // This page will load users via JavaScript/API calls
        return "users";
    }


    @GetMapping("/roles")
    @ResponseBody
    public List<RoleDTO> getRoles(Authentication authentication) {
        return graphApiService.getDirectoryRoles(authentication);
    }

    // Security Groups API (returns JSON)
    @GetMapping("/groups")
    @ResponseBody
    public List<GroupDTO> getSecurityGroups(Authentication authentication) {
        return graphApiService.getSecurityGroups(authentication);
    }
}