# Secret Management Guide

## 🔐 **Overview**

This guide explains how to manage and update Kubernetes secrets for Azure AD credentials used by both services.

---

## 📋 **Current Secret Structure**

The `container-entra-auth-secret` contains:
- `AZURE_CLIENT_ID` - Azure Application (client) ID
- `AZURE_CLIENT_SECRET` - Azure Client Secret
- `AZURE_TENANT_ID` - Azure Tenant ID
- `SPRING_PROFILES_ACTIVE` - Spring Boot profile (k8s)
- `LOGGING_LEVEL_ROOT` - General logging level
- `LOGGING_LEVEL_SECURITY` - Security logging level
- `SERVER_PORT` - Server port configuration

---

## 🔄 **Method 1: Update via .env File (Recommended)**

### **Step 1: Update .env File**
```bash
# Edit the .env file
nano .env

# Update the values you want to change:
AZURE_CLIENT_ID=new-client-id-here
AZURE_CLIENT_SECRET=new-client-secret-here
AZURE_TENANT_ID=new-tenant-id-here
```

### **Step 2: Recreate Secret**
```bash
./scripts/create-secret.sh
```

### **Step 3: Restart Services**
```bash
kubectl rollout restart deployment/container-entra-auth -n dev
kubectl rollout restart deployment/container-login-service -n dev
```

### **Step 4: Verify Restart**
```bash
kubectl rollout status deployment/container-entra-auth -n dev
kubectl rollout status deployment/container-login-service -n dev
```

---

## 🔧 **Method 2: Direct kubectl Commands**

### **Delete and Recreate Secret:**
```bash
# Delete existing secret
kubectl delete secret container-entra-auth-secret -n dev

# Create new secret with updated values
kubectl create secret generic container-entra-auth-secret -n dev \
  --from-literal=AZURE_CLIENT_ID="new-client-id" \
  --from-literal=AZURE_CLIENT_SECRET="new-client-secret" \
  --from-literal=AZURE_TENANT_ID="new-tenant-id" \
  --from-literal=SPRING_PROFILES_ACTIVE="k8s" \
  --from-literal=LOGGING_LEVEL_ROOT="INFO" \
  --from-literal=LOGGING_LEVEL_SECURITY="DEBUG" \
  --from-literal=SERVER_PORT="8080"

# Restart services
kubectl rollout restart deployment/container-entra-auth -n dev
kubectl rollout restart deployment/container-login-service -n dev
```

---

## 🎯 **Method 3: Update Individual Values**

### **Update Single Secret Value:**
```bash
# Update Azure Client ID
kubectl patch secret container-entra-auth-secret -n dev \
  --type='json' \
  -p='[{"op": "replace", "path": "/data/AZURE_CLIENT_ID", "value":"'$(echo -n "new-client-id" | base64)'"}]'

# Update Azure Client Secret
kubectl patch secret container-entra-auth-secret -n dev \
  --type='json' \
  -p='[{"op": "replace", "path": "/data/AZURE_CLIENT_SECRET", "value":"'$(echo -n "new-client-secret" | base64)'"}]'

# Restart services to pick up changes
kubectl rollout restart deployment/container-entra-auth -n dev
kubectl rollout restart deployment/container-login-service -n dev
```

---

## 🔍 **Verification Commands**

### **Check Secret Contents:**
```bash
# View secret (base64 encoded)
kubectl get secret container-entra-auth-secret -n dev -o yaml

# Decode and view specific value
kubectl get secret container-entra-auth-secret -n dev -o jsonpath='{.data.AZURE_CLIENT_ID}' | base64 -d
echo  # Add newline

# View all decoded values
kubectl get secret container-entra-auth-secret -n dev -o json | jq -r '.data | to_entries[] | "\(.key): \(.value | @base64d)"'
```

### **Check Pod Status:**
```bash
# Check if pods restarted successfully
kubectl get pods -n dev

# Check pod logs for any errors
kubectl logs -n dev -l app=container-entra-auth --tail=20
kubectl logs -n dev -l app=container-login-service --tail=20
```

### **Test Updated Credentials:**
```bash
# Test health check
curl http://localhost:8089/health-check

# Test login with new credentials
curl -X POST http://localhost:8089/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "your-email@tenant.onmicrosoft.com", "password": "your-password"}'
```

---

## ⚠️ **Important Notes**

### **Security Best Practices:**
- Always use Method 1 (.env file) for consistency
- Never commit .env file to version control
- Rotate secrets regularly (every 6-12 months)
- Use strong, unique client secrets

### **Service Restart Required:**
- Services must be restarted to pick up new secret values
- Kubernetes doesn't automatically restart pods when secrets change
- Use `kubectl rollout restart` for zero-downtime updates

### **Troubleshooting:**
- If login fails after secret update, check Azure AD app registration
- Verify new client secret is correct and not expired
- Ensure tenant ID matches your Azure AD directory
- Check pod logs for authentication errors

---

## 🚀 **Quick Reference**

### **Complete Update Workflow:**
```bash
# 1. Update credentials
nano .env

# 2. Recreate secret
./scripts/create-secret.sh

# 3. Restart services
kubectl rollout restart deployment/container-entra-auth -n dev
kubectl rollout restart deployment/container-login-service -n dev

# 4. Verify
kubectl get pods -n dev
curl http://localhost:8089/health-check
```

### **Emergency Rollback:**
```bash
# If new credentials don't work, restore from backup .env
cp .env.backup .env
./scripts/create-secret.sh
kubectl rollout restart deployment/container-entra-auth -n dev
kubectl rollout restart deployment/container-login-service -n dev
```

---

## 📁 **Related Files**

- `.env` - Environment variables (not in git)
- `scripts/create-secret.sh` - Secret creation script
- `scripts/deploy-azure-services.sh` - Main deployment script
- `ENV-SETUP-GUIDE.md` - Initial environment setup
- `README.md` - Main deployment guide

**Always backup your working .env file before making changes!** 🔒
