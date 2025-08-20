# Azure Graph API - Complete Deployment Guide

## 🚀 **Quick Start - One Command Deployment**

```bash
./scripts/deploy-azure-services.sh
```

This single command deploys the complete Azure Graph API and Container Login microservices stack with DNS-based service discovery.

---

## 📋 **What deploy-azure-services.sh Does**

### **🎯 Complete Orchestration:**
1. **Minikube Setup** - Creates Kubernetes cluster with Docker driver
2. **Secret Management** - Creates shared Azure credentials secret
3. **Azure Graph API Service** - Builds and deploys backend service
4. **Container Login Service** - Builds and deploys frontend gateway
5. **Port Forwarding** - Exposes services to localhost
6. **Health Verification** - Confirms all services are running

### **✅ End Result:**
- **Azure Graph API Service**: `http://localhost:8087` (backend)
- **Container Login Service**: `http://localhost:8089` (frontend gateway)
- **DNS Resolution**: Services communicate using `container-entra-auth-service:8080`
- **Single Replica**: Each service runs exactly 1 pod
- **Health Checks**: All pods healthy and ready

---

## 🔧 **Prerequisites**

### **Required Tools:**
- **Docker** - Container runtime
- **Minikube** - Local Kubernetes cluster
- **kubectl** - Kubernetes CLI
- **Java 17+** - For building Spring Boot applications
- **Maven** - For dependency management

### **Configuration:**
- **`.env` file** - Contains Azure AD credentials and configuration
- **Kubernetes manifests** - In `k8s/` and `container-login-service/k8s/` directories

---

## 📊 **Architecture Overview**

```
┌─────────────────────────────────────────────────────────────┐
│                    Minikube Cluster                         │
│                                                             │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │ Container Login     │    │ Azure Graph API             │ │
│  │ Service             │────│ Service                     │ │
│  │ (Frontend Gateway)  │    │ (Backend)                   │ │
│  │ Port: 8080          │    │ Port: 8080                  │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
│           │                              │                  │
│           │                              │                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Shared Secret                              │ │
│  │         (container-entra-auth-secret)                   │ │
│  │     Contains: Azure AD credentials, logging config     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
           │                              │
           │                              │
    Port Forward                   Port Forward
    localhost:8089                 localhost:8087
```

---

## 🎯 **Service Communication**

### **DNS-Based Service Discovery:**
- Container Login Service calls: `http://container-entra-auth-service:8080`
- No hardcoded IPs or external URLs
- Kubernetes DNS automatically resolves service names
- Follows 12-Factor App principles

### **API Flow:**
1. **User** → `localhost:8089` → **Container Login Service**
2. **Container Login Service** → `container-entra-auth-service:8080` → **Azure Graph API Service**
3. **Azure Graph API Service** → **Microsoft Graph API** → **Azure AD**

---

## 🧪 **Testing the Deployment**

### **1. Health Check:**
```bash
curl http://localhost:8089/health-check
```

### **2. Login:**
```bash
curl -X POST http://localhost:8089/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "maris@aravindmaklabsoutlook.onmicrosoft.com", "password": "changeme"}'
```

### **3. Use Postman Collection:**
Import `postman/Azure-Graph-API-Simple.postman_collection.json` for complete API testing.

---

## 🔄 **Cleanup and Redeploy**

### **Full Cleanup:**
```bash
minikube delete
docker system prune -a -f
./mvnw clean && cd container-login-service && ./mvnw clean
```

### **Fresh Deploy:**
```bash
./scripts/deploy-azure-services.sh
```

## 🔐 **Secret Management**

### **Update Azure AD Credentials:**
```bash
# Edit .env file with new credentials
nano .env

# Recreate secret and restart services
./scripts/create-secret.sh
kubectl rollout restart deployment/container-entra-auth -n dev
kubectl rollout restart deployment/container-login-service -n dev
```

**For detailed secret management:** See `SECRET-MANAGEMENT.md`

---

## 🚨 **Troubleshooting**

### **Services Not Starting:**
```bash
kubectl get pods -n dev
kubectl logs -n dev <pod-name>
```

### **DNS Issues:**
```bash
kubectl get services -n dev
kubectl exec -n dev <pod-name> -- nslookup container-entra-auth-service
```

### **Port Forwarding Issues:**
```bash
kubectl port-forward -n dev service/container-login-service 8089:8080 &
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 &
```

---

## 📁 **Project Structure**

```
azure-graph-api-test/
├── scripts/
│   └── deploy-azure-services.sh  ← Master deployment script
├── container-login-service/       ← Frontend gateway service
│   └── Dockerfile                 ← Container build (k8s profile)
├── postman/                       ← API testing collection
├── k8s/                          ← Azure Graph API Kubernetes manifests
├── Dockerfile.k8s                 ← Azure Graph API container build (k8s profile)
├── .env                          ← Configuration (Azure credentials)
├── ENV-SETUP-GUIDE.md            ← .env file creation guide
├── SECRET-MANAGEMENT.md          ← Secret update and management guide
├── MANUAL-DEPLOYMENT.md          ← Step-by-step deployment guide
└── README.md                     ← This file
```

## ⚙️ **Configuration Consistency**

Both services use consistent configuration:
- **Spring Profile**: `k8s` (set in both Dockerfiles and .env)
- **Container Optimization**: JVM settings optimized for Kubernetes
- **Health Checks**: Both services include health check endpoints
- **12-Factor Compliance**: Stateless, disposable processes

---

## 🎉 **Success Indicators**

✅ **Minikube Status**: `minikube status` shows "Running"  
✅ **Pods Ready**: All pods show "1/1 Running"  
✅ **Services Available**: Both localhost:8089 and localhost:8087 respond  
✅ **DNS Working**: Health check shows Azure service connection success  
✅ **Authentication**: Login returns session code  
✅ **API Calls**: All endpoints return expected data  

**Ready for development and testing!** 🚀
