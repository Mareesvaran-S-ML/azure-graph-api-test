# .env File Configuration Guide

## 🎯 **Purpose**

The `.env` file contains Azure AD configuration and service settings. It's used to create Kubernetes secrets that both services share.

---

## 📋 **.env File Template**

Create a `.env` file in the root directory:

```bash
# Azure OAuth2 Configuration
# Copy this file to .env and fill in your actual values

# Required: Azure Application (client) ID
AZURE_CLIENT_ID=your-client-id-here

# Required: Azure Client Secret
AZURE_CLIENT_SECRET=your-client-secret-here

# Required: Azure Tenant ID
AZURE_TENANT_ID=your-tenant-id-here

# Optional: Spring Boot Profile
SPRING_PROFILES_ACTIVE=k8s

# Optional: Logging Levels
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_SECURITY=DEBUG


```

---

## 🔧 **Getting Azure AD Values**

### **Step 1: Azure Portal**
1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**

### **Step 2: Get Values**

#### **AZURE_CLIENT_ID:**
- From app registration **Overview** page
- Copy **Application (client) ID**

#### **AZURE_TENANT_ID:**
- From app registration **Overview** page  
- Copy **Directory (tenant) ID**

#### **AZURE_CLIENT_SECRET:**
- Go to **Certificates & secrets**
- Create **New client secret**
- Copy the secret value immediately

---

## 📝 **Configuration Details**

### **Required Variables:**
- `AZURE_CLIENT_ID` - Your Azure app registration ID
- `AZURE_CLIENT_SECRET` - Secret for authentication
- `AZURE_TENANT_ID` - Your Azure AD tenant ID

### **Optional Variables:**
- `SPRING_PROFILES_ACTIVE=k8s` - Uses Kubernetes configuration (consistent across both services)
- `LOGGING_LEVEL_ROOT=INFO` - General application logging
- `LOGGING_LEVEL_SECURITY=DEBUG` - Security/auth logging

### **Profile Consistency:**
Both services use the `k8s` profile for:
- Kubernetes-optimized configuration
- Container health checks
- JVM optimization settings
- 12-Factor App compliance

### **Service Communication:**
- Inter-service communication uses default DNS names
- Container Login Service → Azure Graph API Service via `container-entra-auth-service:8080`
- No additional configuration needed

---

## 🚨 **Security Notes**

- Keep `.env` file in `.gitignore`
- Never commit secrets to version control
- Rotate client secrets regularly

---

## ✅ **Verification**

After creating `.env` file:

```bash
# Deploy services
./scripts/deploy-azure-services.sh

# Test health check
curl http://localhost:8089/health-check
```

**File should be placed at:** `azure-graph-api-test/.env`
