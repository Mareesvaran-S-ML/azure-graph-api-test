# Manual Step-by-Step Deployment Guide

## 🎯 **When to Use This Guide**

Use this manual approach when you want to:
- **Understand each deployment step** in detail
- **Debug specific issues** with individual services
- **Deploy services separately** for development
- **Learn the deployment process** before using automation

---

## 📋 **Step-by-Step Deployment Process**

### **Step 1: Setup Minikube Cluster**

**What it does:** Creates a local Kubernetes cluster with Docker driver and DNS resolution.

**Why:** Provides the foundation for running containerized microservices with service discovery.

```bash
./scripts/setup-minikube.sh
```

**Expected Output:**
```
✅ Minikube cluster created successfully
✅ Docker driver configured
✅ CoreDNS ready for service discovery
```

---

### **Step 2: Create Shared Secret**

**What it does:** Creates Kubernetes secret from `.env` file containing Azure AD credentials.

**Why:** Provides secure, centralized configuration for both services following 12-Factor principles.

```bash
./scripts/create-secret.sh
```

**Expected Output:**
```
✅ Secret 'container-entra-auth-secret' created in namespace 'dev'
✅ Contains: Azure credentials, logging config, server settings
```

---

### **Step 3: Deploy Azure Graph API Service (Backend)**

**What it does:** 
- Builds Spring Boot JAR
- Creates Docker image
- Loads image into Minikube
- Deploys to Kubernetes
- Exposes service internally

**Why:** This is the backend service that communicates with Microsoft Graph API.

```bash
# Build the service
./scripts/build-k8s.sh

# Deploy to Kubernetes
./scripts/deploy-k8s.sh
```

**Expected Output:**
```
✅ JAR built successfully
✅ Docker image created: container-entra-auth:latest
✅ Image loaded into Minikube
✅ Deployed to namespace 'dev'
✅ Service available at: container-entra-auth-service:8080
```

**Verify Deployment:**
```bash
kubectl get pods -n dev -l app.kubernetes.io/name=container-entra-auth
kubectl get services -n dev container-entra-auth-service
```

---

### **Step 4: Deploy Container Login Service (Frontend Gateway)**

**What it does:**
- Builds Spring Boot JAR
- Creates Docker image
- Loads image into Minikube
- Loads Azure Graph API image (if needed)
- Deploys to Kubernetes
- Exposes service internally

**Why:** This is the frontend gateway that provides web API endpoints and communicates with the backend.

```bash
cd container-login-service
./scripts/build-and-deploy.sh
cd ..
```

**Expected Output:**
```
✅ JAR built successfully
✅ Docker image created: container-login-service:latest
✅ Image loaded into Minikube
✅ Azure Graph API image also loaded
✅ Deployed to namespace 'dev'
✅ Service available at: container-login-service:8080
```

**Verify Deployment:**
```bash
kubectl get pods -n dev -l app.kubernetes.io/name=container-login-service
kubectl get services -n dev container-login-service
```

---

### **Step 5: Setup Port Forwarding**

**What it does:** Exposes Kubernetes services to localhost for external access.

**Why:** Allows testing and interaction with services from your local machine.

```bash
# Forward Container Login Service (Frontend)
kubectl port-forward -n dev service/container-login-service 8089:8080 &

# Forward Azure Graph API Service (Backend) 
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 &
```

**Expected Output:**
```
Forwarding from 127.0.0.1:8089 -> 8080
Forwarding from 127.0.0.1:8087 -> 8080
```

---

### **Step 6: Verify Complete Deployment**

**What it does:** Tests that all services are running and communicating properly.

**Why:** Confirms the entire stack is working with DNS-based service discovery.

```bash
# Test health check (includes DNS resolution test)
curl http://localhost:8089/health-check

# Test login functionality
curl -X POST http://localhost:8089/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "maris@aravindmaklabsoutlook.onmicrosoft.com", "password": "changeme"}'
```

**Expected Output:**
```json
{
  "status": "SUCCESS",
  "azure_service_connection": "SUCCESS",
  "dns_resolution": "container-entra-auth-service resolves correctly"
}
```

---

## 🔍 **Understanding Each Component**

### **Azure Graph API Service (Backend):**
- **Purpose:** Communicates with Microsoft Graph API
- **Port:** 8080 (internal), 8087 (external via port-forward)
- **DNS Name:** `container-entra-auth-service`
- **Endpoints:** `/api/graph/*` (internal API)

### **Container Login Service (Frontend Gateway):**
- **Purpose:** Provides web API endpoints, proxies to backend
- **Port:** 8080 (internal), 8089 (external via port-forward)
- **DNS Name:** `container-login-service`
- **Endpoints:** `/web/*` (public API)

### **Service Communication:**
```
User Request → localhost:8089 → Container Login Service → 
container-entra-auth-service:8080 → Azure Graph API Service → Microsoft Graph API
```

---

## 🚨 **Troubleshooting Individual Steps**

### **Step 1 Issues (Minikube):**
```bash
minikube status
minikube logs
```

### **Step 2 Issues (Secret):**
```bash
kubectl get secrets -n dev
kubectl describe secret container-entra-auth-secret -n dev
```

### **Step 3 Issues (Azure Graph API):**
```bash
kubectl get pods -n dev -l app.kubernetes.io/name=container-entra-auth
kubectl logs -n dev <azure-pod-name>
```

### **Step 4 Issues (Container Login Service):**
```bash
kubectl get pods -n dev -l app.kubernetes.io/name=container-login-service
kubectl logs -n dev <login-pod-name>
```

### **Step 5 Issues (Port Forwarding):**
```bash
# Kill existing port forwards
pkill -f "kubectl port-forward"

# Restart port forwarding
kubectl port-forward -n dev service/container-login-service 8089:8080 &
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 &
```

---

## 🎯 **Benefits of Manual Deployment**

✅ **Educational:** Understand each component and its role  
✅ **Debugging:** Isolate issues to specific services  
✅ **Flexibility:** Deploy only what you need for testing  
✅ **Control:** Full visibility into each deployment step  

---

## 🚀 **Quick Commands Summary**

```bash
# Full manual deployment
./scripts/setup-minikube.sh
./scripts/create-secret.sh
./scripts/build-k8s.sh && ./scripts/deploy-k8s.sh
cd container-login-service && ./scripts/build-and-deploy.sh && cd ..
kubectl port-forward -n dev service/container-login-service 8089:8080 &
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 &

# Test deployment
curl http://localhost:8089/health-check
```

**For automated deployment, use:** `./scripts/deploy-azure-services.sh` 🎉
