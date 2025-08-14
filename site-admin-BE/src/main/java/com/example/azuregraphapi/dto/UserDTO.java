package com.example.azuregraphapi.dto;

import java.util.List;

public class UserDTO {
    private String id;
    private String displayName;
    private String userPrincipalName;
    private String mail;
    private String jobTitle;
    private String department;
    private Boolean accountEnabled;
    private String createdDateTime;
    private String lastSignInDateTime;
    private String userType;
    private List<String> roles;
    private List<GroupDTO> groups;

    // Constructors
    public UserDTO() {}

    public UserDTO(String id, String displayName, String userPrincipalName, String mail) {
        this.id = id;
        this.displayName = displayName;
        this.userPrincipalName = userPrincipalName;
        this.mail = mail;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getUserPrincipalName() { return userPrincipalName; }
    public void setUserPrincipalName(String userPrincipalName) { this.userPrincipalName = userPrincipalName; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Boolean getAccountEnabled() { return accountEnabled; }
    public void setAccountEnabled(Boolean accountEnabled) { this.accountEnabled = accountEnabled; }

    public String getCreatedDateTime() { return createdDateTime; }
    public void setCreatedDateTime(String createdDateTime) { this.createdDateTime = createdDateTime; }

    public String getLastSignInDateTime() { return lastSignInDateTime; }
    public void setLastSignInDateTime(String lastSignInDateTime) { this.lastSignInDateTime = lastSignInDateTime; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<GroupDTO> getGroups() { return groups; }
    public void setGroups(List<GroupDTO> groups) { this.groups = groups; }
}

