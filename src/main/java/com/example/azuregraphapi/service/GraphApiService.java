package com.example.azuregraphapi.service;

import com.example.azuregraphapi.dto.UserDTO;
import com.example.azuregraphapi.dto.GroupDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphApiService {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final WebClient webClient;

    public GraphApiService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
        this.webClient = WebClient.builder()
                .baseUrl("https://graph.microsoft.com/v1.0")
                .build();
    }

    private String getAccessToken(Authentication authentication) {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        return client.getAccessToken().getTokenValue();
    }

    public UserDTO getCurrentUser(Authentication authentication) {
        try {
            String accessToken = getAccessToken(authentication);

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
                List<GroupDTO> groups = getUserGroups(authentication, null);
                userDTO.setGroups(groups != null ? groups : new ArrayList<>());
            } catch (Exception e) {
                System.out.println("Warning: Could not retrieve user groups - " + e.getMessage());
                userDTO.setGroups(new ArrayList<>());
            }

            // Get user's directory roles
            try {
                List<String> roles = getUserRoles(authentication, null);
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

    public List<GroupDTO> getUserGroups(Authentication authentication, String userId) {
        try {
            String accessToken = getAccessToken(authentication);
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

    public List<String> getUserRoles(Authentication authentication, String userId) {
        try {
            String accessToken = getAccessToken(authentication);
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

    public List<UserDTO> getAllUsers(Authentication authentication) {
        try {
            String accessToken = getAccessToken(authentication);

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
                        List<GroupDTO> groups = getUserGroups(authentication, userId);
                        List<String> roles = getUserRoles(authentication, userId);
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


}
