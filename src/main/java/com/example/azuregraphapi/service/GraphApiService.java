package com.example.azuregraphapi.service;

import com.example.azuregraphapi.dto.RoleDTO;
import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.dto.GroupDTO;
import com.example.azuregraphapi.config.AzureProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class GraphApiService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final WebClient webClient;
    private final AzureProperties azureProperties;

    public GraphApiService(OAuth2AuthorizedClientService authorizedClientService, AzureProperties azureProperties) {
        this.authorizedClientService = authorizedClientService;
        this.azureProperties = azureProperties;
        this.webClient = WebClient.builder()
                .baseUrl("https://graph.microsoft.com/v1.0")
                .build();
    }

    private String getAccessToken(Authentication authentication) {
        // Check if this is OAuth2 authentication
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );
            return client.getAccessToken().getTokenValue();
        }

        // If not OAuth2, this should not be called - throw exception
        throw new RuntimeException("Invalid authentication type for getAccessToken");
    }

    private String getAccessTokenFromSession(HttpServletRequest request) {
        // Get access token from session (stored during login)
        Object token = request.getSession().getAttribute("azure_access_token");
        if (token != null) {
            return token.toString();
        }
        throw new RuntimeException("No access token found in session");
    }

    public UserDTO getCurrentUser(Authentication authentication, HttpServletRequest request) {
        try {
            String accessToken;
            if (authentication != null && authentication instanceof OAuth2AuthenticationToken) {
                accessToken = getAccessToken(authentication);
            } else {
                // Use session-based token
                accessToken = getAccessTokenFromSession(request);
            }

            // Get user profile with all fields
            Mono<JsonNode> userMono = webClient.get()
                    .uri("/me?$select=id,displayName,userPrincipalName,mail,jobTitle,department,accountEnabled,createdDateTime,lastSignInDateTime,userType,assignedLicenses")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode userJson = userMono.block();

            UserDTO userDTO = new UserDTO();
            userDTO.setId(userJson.get("id").asText());
            userDTO.setDisplayName(userJson.has("displayName") && !userJson.get("displayName").isNull() ? userJson.get("displayName").asText() : null);
            userDTO.setUserPrincipalName(userJson.has("userPrincipalName") && !userJson.get("userPrincipalName").isNull() ? userJson.get("userPrincipalName").asText() : null);
            userDTO.setMail(userJson.has("mail") && !userJson.get("mail").isNull() ? userJson.get("mail").asText() : null);
            userDTO.setJobTitle(userJson.has("jobTitle") && !userJson.get("jobTitle").isNull() ? userJson.get("jobTitle").asText() : null);
            userDTO.setDepartment(userJson.has("department") && !userJson.get("department").isNull() ? userJson.get("department").asText() : null);
            userDTO.setAccountEnabled(userJson.has("accountEnabled") && !userJson.get("accountEnabled").isNull() ? userJson.get("accountEnabled").asBoolean() : true);
            userDTO.setCreatedDateTime(userJson.has("createdDateTime") && !userJson.get("createdDateTime").isNull() ? userJson.get("createdDateTime").asText() : null);
            userDTO.setLastSignInDateTime(userJson.has("lastSignInDateTime") && !userJson.get("lastSignInDateTime").isNull() ? userJson.get("lastSignInDateTime").asText() : null);
            userDTO.setUserType(userJson.has("userType") && !userJson.get("userType").isNull() ? userJson.get("userType").asText() : "Member");

            // Get user's group memberships
            try {
                List<GroupDTO> groups = getUserGroups(authentication, request, null);
                userDTO.setGroups(groups != null ? groups : new ArrayList<>());
            } catch (Exception e) {
                System.out.println("Warning: Could not retrieve user groups - " + e.getMessage());
                userDTO.setGroups(new ArrayList<>());
            }

            // Get user's directory roles
            try {
                List<String> roles = getUserRoles(authentication, request, null);
                userDTO.setRoles(roles != null ? roles : new ArrayList<>());
            } catch (Exception e) {
                System.out.println("Warning: Could not retrieve user roles - " + e.getMessage());
                userDTO.setRoles(new ArrayList<>());
            }

            return userDTO;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve user information: " + e.getMessage(), e);
        }
    }

    public List<GroupDTO> getUserGroups(Authentication authentication, HttpServletRequest request, String userId) {
        try {
            String accessToken;
            if (authentication != null && authentication instanceof OAuth2AuthenticationToken) {
                accessToken = getAccessToken(authentication);
            } else {
                // Use session-based token
                accessToken = getAccessTokenFromSession(request);
            }
            String uri = userId != null ? "/users/" + userId + "/memberOf" : "/me/memberOf";

            Mono<JsonNode> groupsMono = webClient.get()
                    .uri(uri + "?$select=id,displayName,description,groupTypes")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode groupsJson = groupsMono.block();
            List<GroupDTO> groups = new ArrayList<>();

            if (groupsJson != null && groupsJson.has("value")) {
                JsonNode groupsArray = groupsJson.get("value");
                for (JsonNode groupNode : groupsArray) {
                    // Only include groups (not directory roles)
                    if (groupNode.has("@odata.type") &&
                            groupNode.get("@odata.type").asText().contains("group")) {

                        GroupDTO groupDTO = new GroupDTO();
                        groupDTO.setId(groupNode.get("id").asText());
                        groupDTO.setDisplayName(groupNode.has("displayName") && !groupNode.get("displayName").isNull() ?
                                groupNode.get("displayName").asText() : null);
                        groupDTO.setDescription(groupNode.has("description") && !groupNode.get("description").isNull() ?
                                groupNode.get("description").asText() : null);

                        // Set group type based on groupTypes array
                        if (groupNode.has("groupTypes") && !groupNode.get("groupTypes").isNull()) {
                            JsonNode groupTypesArray = groupNode.get("groupTypes");
                            if (groupTypesArray.isArray() && groupTypesArray.size() > 0) {
                                // Microsoft 365 groups have "Unified" in groupTypes
                                boolean isUnified = false;
                                for (JsonNode typeNode : groupTypesArray) {
                                    if ("Unified".equals(typeNode.asText())) {
                                        isUnified = true;
                                        break;
                                    }
                                }
                                groupDTO.setGroupType(isUnified ? "Microsoft 365" : "Security");
                            } else {
                                groupDTO.setGroupType("Security");
                            }
                        } else {
                            groupDTO.setGroupType("Security");
                        }

                        groups.add(groupDTO);
                    }
                }
            }

            return groups;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve user groups: " + e.getMessage(), e);
        }
    }

    public List<String> getUserRoles(Authentication authentication, HttpServletRequest request, String userId) {
        try {
            String accessToken;
            if (authentication != null && authentication instanceof OAuth2AuthenticationToken) {
                accessToken = getAccessToken(authentication);
            } else {
                // Use session-based token
                accessToken = getAccessTokenFromSession(request);
            }
            String uri = userId != null ? "/users/" + userId + "/memberOf" : "/me/memberOf";

            Mono<JsonNode> rolesMono = webClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode rolesJson = rolesMono.block();
            List<String> roles = new ArrayList<>();

            if (rolesJson != null && rolesJson.has("value")) {
                JsonNode rolesArray = rolesJson.get("value");
                for (JsonNode roleNode : rolesArray) {
                    // Only include directory roles
                    if (roleNode.has("@odata.type") &&
                            roleNode.get("@odata.type").asText().contains("directoryRole")) {

                        String roleName = roleNode.has("displayName") ?
                                roleNode.get("displayName").asText() : "Unknown Role";
                        roles.add(roleName);
                    }
                }
            }

            return roles;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve user roles: " + e.getMessage(), e);
        }
    }

    public List<UserDTO> getAllUsers(Authentication authentication, HttpServletRequest request) {
        try {
            String accessToken;
            if (authentication != null && authentication instanceof OAuth2AuthenticationToken) {
                accessToken = getAccessToken(authentication);
            } else {
                // Use session-based token
                accessToken = getAccessTokenFromSession(request);
            }

            Mono<JsonNode> usersMono = webClient.get()
                    .uri("/users?$top=100&$select=id,displayName,userPrincipalName,mail,jobTitle,department,accountEnabled,createdDateTime,lastSignInDateTime,userType,assignedLicenses")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode usersJson = usersMono.block();
            List<UserDTO> userDTOs = new ArrayList<>();

            if (usersJson != null && usersJson.has("value")) {
                JsonNode usersArray = usersJson.get("value");
                for (JsonNode userNode : usersArray) {
                    UserDTO userDTO = new UserDTO();
                    String userId = userNode.get("id").asText();

                    userDTO.setId(userId);
                    userDTO.setDisplayName(userNode.has("displayName") ?
                            userNode.get("displayName").asText() : null);
                    userDTO.setUserPrincipalName(userNode.has("userPrincipalName") ?
                            userNode.get("userPrincipalName").asText() : null);
                    userDTO.setMail(userNode.has("mail") ?
                            userNode.get("mail").asText() : null);
                    userDTO.setJobTitle(userNode.has("jobTitle") ?
                            userNode.get("jobTitle").asText() : null);
                    userDTO.setDepartment(userNode.has("department") ?
                            userNode.get("department").asText() : null);
                    userDTO.setAccountEnabled(userNode.has("accountEnabled") ?
                            userNode.get("accountEnabled").asBoolean() : true);
                    userDTO.setCreatedDateTime(userNode.has("createdDateTime") ?
                            userNode.get("createdDateTime").asText() : null);
                    userDTO.setLastSignInDateTime(userNode.has("lastSignInDateTime") ?
                            userNode.get("lastSignInDateTime").asText() : null);
                    userDTO.setUserType(userNode.has("userType") ?
                            userNode.get("userType").asText() : "Member");

                    // Get groups and roles for each user
                    try {
                        List<GroupDTO> groups = getUserGroups(authentication, request, userId);
                        List<String> roles = getUserRoles(authentication, request, userId);
                        userDTO.setGroups(groups);
                        userDTO.setRoles(roles);
                    } catch (Exception e) {
                        // If we can't get groups/roles for a user, set empty lists
                        userDTO.setGroups(new ArrayList<>());
                        userDTO.setRoles(new ArrayList<>());
                    }

                    userDTOs.add(userDTO);
                }
            }

            return userDTOs;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all users: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticate with Azure AD using username and password (Resource Owner Password Credentials flow)
     */
    public Map<String, Object> authenticateWithCredentials(String username, String password) {
        System.out.println("Attempting authentication for user: " + username);
        try {
            // Get configuration from application.yml
            String tokenUrl = azureProperties.getProvider().getAzure().getTokenUri();
            String clientId = azureProperties.getRegistration().getAzure().getClientId();
            String clientSecret = azureProperties.getRegistration().getAzure().getClientSecret();
            java.util.List<String> scopes = azureProperties.getRegistration().getAzure().getScope();

            System.out.println("Token URL: " + tokenUrl);
            System.out.println("Client ID: " + clientId);
            System.out.println("Scopes: " + scopes);

            // Build scope string - convert list to space-separated string
            String scopeString = String.join(" ", scopes);

            // Prepare request body for Resource Owner Password Credentials flow
            String requestBody = "grant_type=password" +
                    "&client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8") +
                    "&client_secret=" + java.net.URLEncoder.encode(clientSecret, "UTF-8") +
                    "&scope=" + java.net.URLEncoder.encode(scopeString, "UTF-8") +
                    "&username=" + java.net.URLEncoder.encode(username, "UTF-8") +
                    "&password=" + java.net.URLEncoder.encode(password, "UTF-8");

            // Make token request
            Mono<JsonNode> tokenMono = WebClient.create()
                    .post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode tokenResponse = tokenMono.block();
            System.out.println("Token response: " + (tokenResponse != null ? tokenResponse.toString() : "null"));

            if (tokenResponse != null && tokenResponse.has("access_token")) {
                String accessToken = tokenResponse.get("access_token").asText();

                // Get user info using the access token
                Mono<JsonNode> userMono = webClient.get()
                        .uri("/me?$select=id,displayName,userPrincipalName,mail")
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class);

                JsonNode userJson = userMono.block();

                if (userJson != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("authenticated", true);
                    result.put("access_token", accessToken);
                    result.put("user_id", userJson.get("userPrincipalName").asText());
                    result.put("display_name", userJson.get("displayName").asText());

                    // Calculate expiry time (default 1 hour)
                    long expiresIn = tokenResponse.has("expires_in") ?
                            tokenResponse.get("expires_in").asLong() : 3600;
                    result.put("expires_at", java.time.Instant.now().plusSeconds(expiresIn));

                    return result;
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("authenticated", false);
                    result.put("error", "Failed to get user information");
                    return result;
                }
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("authenticated", false);
                result.put("error", tokenResponse != null && tokenResponse.has("error_description") ?
                        tokenResponse.get("error_description").asText() : "Invalid credentials");
                return result;
            }

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("authenticated", false);
            result.put("error", "Authentication failed: " + e.getMessage());
            return result;
        }
    }



    public List<RoleDTO> getDirectoryRoles(Authentication authentication) {
        try {
            String accessToken = getAccessToken(authentication);

            Mono<JsonNode> rolesMono = webClient.get()
                    .uri("/directoryRoles")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode rolesJson = rolesMono.block();
            List<RoleDTO> roles = new ArrayList<>();

            if (rolesJson != null && rolesJson.has("value")) {
                JsonNode rolesArray = rolesJson.get("value");
                for (JsonNode roleNode : rolesArray) {
                    RoleDTO roleDTO = new RoleDTO();
                    roleDTO.setId(roleNode.has("id") ? roleNode.get("id").asText() : null);
                    roleDTO.setDisplayName(roleNode.has("displayName") ? roleNode.get("displayName").asText() : null);
                    roleDTO.setDescription(roleNode.has("description") ? roleNode.get("description").asText() : null);
                    roles.add(roleDTO);
                }
            }

            return roles;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve directory roles: " + e.getMessage(), e);
        }
    }

    public List<GroupDTO> getSecurityGroups(Authentication authentication) {
        try {
            String accessToken = getAccessToken(authentication);

            Mono<JsonNode> groupsMono = webClient.get()
                    .uri("/groups?$filter=securityEnabled eq true&$select=id,displayName,description")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class);

            JsonNode groupsJson = groupsMono.block();
            List<GroupDTO> groups = new ArrayList<>();

            if (groupsJson != null && groupsJson.has("value")) {
                JsonNode groupsArray = groupsJson.get("value");
                for (JsonNode groupNode : groupsArray) {
                    GroupDTO groupDTO = new GroupDTO();
                    groupDTO.setId(groupNode.has("id") ? groupNode.get("id").asText() : null);
                    groupDTO.setDisplayName(groupNode.has("displayName") ? groupNode.get("displayName").asText() : null);
                    groupDTO.setDescription(groupNode.has("description") ? groupNode.get("description").asText() : null);
                    groups.add(groupDTO);
                }
            }

            return groups;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve security groups: " + e.getMessage(), e);
        }
    }
}
