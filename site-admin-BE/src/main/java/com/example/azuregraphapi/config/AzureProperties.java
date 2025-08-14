package com.example.azuregraphapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class AzureProperties {
    
    private Registration registration = new Registration();
    private Provider provider = new Provider();
    
    public Registration getRegistration() {
        return registration;
    }
    
    public void setRegistration(Registration registration) {
        this.registration = registration;
    }
    
    public Provider getProvider() {
        return provider;
    }
    
    public void setProvider(Provider provider) {
        this.provider = provider;
    }
    
    public static class Registration {
        private Azure azure = new Azure();
        
        public Azure getAzure() {
            return azure;
        }
        
        public void setAzure(Azure azure) {
            this.azure = azure;
        }
        
        public static class Azure {
            private String clientId;
            private String clientSecret;
            private List<String> scope;
            private String authorizationGrantType;
            private String redirectUri;
            private String clientName;
            
            public String getClientId() {
                return clientId;
            }
            
            public void setClientId(String clientId) {
                this.clientId = clientId;
            }
            
            public String getClientSecret() {
                return clientSecret;
            }
            
            public void setClientSecret(String clientSecret) {
                this.clientSecret = clientSecret;
            }
            
            public List<String> getScope() {
                return scope;
            }
            
            public void setScope(List<String> scope) {
                this.scope = scope;
            }
            
            public String getAuthorizationGrantType() {
                return authorizationGrantType;
            }
            
            public void setAuthorizationGrantType(String authorizationGrantType) {
                this.authorizationGrantType = authorizationGrantType;
            }
            
            public String getRedirectUri() {
                return redirectUri;
            }
            
            public void setRedirectUri(String redirectUri) {
                this.redirectUri = redirectUri;
            }
            
            public String getClientName() {
                return clientName;
            }
            
            public void setClientName(String clientName) {
                this.clientName = clientName;
            }
        }
    }
    
    public static class Provider {
        private Azure azure = new Azure();
        
        public Azure getAzure() {
            return azure;
        }
        
        public void setAzure(Azure azure) {
            this.azure = azure;
        }
        
        public static class Azure {
            private String authorizationUri;
            private String tokenUri;
            private String userInfoUri;
            private String jwkSetUri;
            private String userNameAttribute;
            
            public String getAuthorizationUri() {
                return authorizationUri;
            }
            
            public void setAuthorizationUri(String authorizationUri) {
                this.authorizationUri = authorizationUri;
            }
            
            public String getTokenUri() {
                return tokenUri;
            }
            
            public void setTokenUri(String tokenUri) {
                this.tokenUri = tokenUri;
            }
            
            public String getUserInfoUri() {
                return userInfoUri;
            }
            
            public void setUserInfoUri(String userInfoUri) {
                this.userInfoUri = userInfoUri;
            }
            
            public String getJwkSetUri() {
                return jwkSetUri;
            }
            
            public void setJwkSetUri(String jwkSetUri) {
                this.jwkSetUri = jwkSetUri;
            }
            
            public String getUserNameAttribute() {
                return userNameAttribute;
            }
            
            public void setUserNameAttribute(String userNameAttribute) {
                this.userNameAttribute = userNameAttribute;
            }
        }
    }
}
