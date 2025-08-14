# WSL External Access Guide

## 🌐 **Accessing Container-Entra-auth from Windows Host**

This guide explains how to access your Kubernetes-deployed Container-Entra-auth application from outside WSL.

## ✅ **Current Setup (Active)**

Your application is currently running with the following access methods:

### **Method 1: Port Forward (Recommended)**
```bash
# Command running in WSL
export KUBECONFIG=/root/.kube/config && kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 --address=0.0.0.0
```

**✅ Status**: ACTIVE  
**✅ Access URL**: `http://localhost:8087`  
**✅ Available from**: Windows host, WSL, and external networks

### **Method 2: WSL IP Direct Access**
**✅ WSL IP**: `172.28.0.35`  
**✅ Access URL**: `http://172.28.0.35:8087`  
**✅ Available from**: Windows host

### **Method 3: NodePort (Internal)**
**✅ NodePort**: `30080`  
**✅ Access URL**: `http://localhost:30080` (from within WSL)

## 🧪 **Testing from Windows**

### **Health Check**
```bash
# From Windows Command Prompt or PowerShell
curl http://localhost:8087/health
```

**Expected Response:**
```json
{"service":"container-entra-auth","status":"UP","timestamp":1755174434541}
```

### **Azure AD Authentication**
```bash
# From Windows Command Prompt or PowerShell
curl -X POST http://localhost:8087/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"maris@aravindmaklabsoutlook.onmicrosoft.com\", \"password\": \"experiment!ngSecret$\"}"
```

**Expected Success Response:**
```json
{
  "authenticated": true,
  "expires_at": "2025-08-14T13:34:59.317540406Z",
  "user_id": "maris@aravindmaklabsoutlook.onmicrosoft.com",
  "session_code": "66C2BD858D5D102AC0F0236AA5FDF857",
  "message": "Authentication successful"
}
```

## 🌐 **Browser Access**

Open these URLs in your Windows browser:

- **Health Check**: http://localhost:8087/health
- **API Base**: http://localhost:8087/api/v1
- **Authentication Endpoint**: http://localhost:8087/api/auth/login (POST)

## 🔧 **Setup Commands for New Sessions**

If you need to restart the port forward:

```bash
# In WSL terminal
export KUBECONFIG=/root/.kube/config
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 --address=0.0.0.0
```

## 🛠️ **Alternative Access Methods**

### **Option A: Ingress Controller (Advanced)**
```bash
# Install NGINX Ingress
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/baremetal/deploy.yaml

# Create ingress resource
kubectl apply -f k8s/ingress.yaml
```

### **Option B: LoadBalancer Service (Cloud)**
```bash
# Change service type to LoadBalancer
kubectl patch service container-entra-auth-service -n dev -p '{"spec":{"type":"LoadBalancer"}}'
```

### **Option C: Host Network (Direct)**
```bash
# Update deployment to use host network
kubectl patch deployment container-entra-auth -n dev -p '{"spec":{"template":{"spec":{"hostNetwork":true}}}}'
```

## 🔍 **Troubleshooting**

### **Port Forward Not Working**
```bash
# Check if socat is installed
which socat

# Install socat if missing
apt-get update && apt-get install -y socat

# Restart port forward
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 --address=0.0.0.0
```

### **Connection Refused**
```bash
# Check pods are running
kubectl get pods -n dev

# Check service endpoints
kubectl get endpoints -n dev

# Check service status
kubectl describe service container-entra-auth-service -n dev
```

### **WSL IP Changed**
```bash
# Get current WSL IP
ip addr show eth0 | grep 'inet ' | awk '{print $2}' | cut -d/ -f1

# Update access URL accordingly
```

## 📋 **Access Summary**

| Method | URL | Status | Use Case |
|--------|-----|--------|----------|
| **Port Forward** | `http://localhost:8087` | ✅ Active | Development, Testing |
| **WSL IP** | `http://172.28.0.35:8087` | ✅ Available | Direct access |
| **NodePort** | `http://localhost:30080` | ✅ Internal | WSL-only access |
| **Browser** | `http://localhost:8087/health` | ✅ Working | Web interface |

## 🎉 **Success!**

Your Container-Entra-auth application is now fully accessible from Windows host with working Azure AD authentication! 🚀

### **Key URLs for External Access:**
- **Health**: http://localhost:8087/health
- **Auth**: http://localhost:8087/api/auth/login
- **API**: http://localhost:8087/api/v1

The port forward is running and ready for external connections!
