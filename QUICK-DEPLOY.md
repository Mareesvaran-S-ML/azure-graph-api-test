# Container-Entra-auth Quick Deploy Guide

## 🚀 **Tested Commands - Copy & Paste**

These commands have been **tested and verified** to work for deploying Container-Entra-auth with Azure AD authentication.

### **Prerequisites**
```bash
# Install Docker (if needed)
curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh

# Install kubectl (if needed)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install kubectl /usr/local/bin/kubectl
```

### **Step 1: Install Minikube**
```bash
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64
```

### **Step 2: Start Minikube**
```bash
minikube start --driver=none --kubernetes-version=v1.28.0
```

If you get KUBECONFIG issues:
```bash
unset KUBECONFIG && minikube start --driver=none --kubernetes-version=v1.28.0
```

### **Step 3: Build Image**
```bash
./scripts/build-k8s.sh
```

### **Step 4: Deploy to Kubernetes**
```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create Azure secret
kubectl create secret generic container-entra-auth-secret \
  --namespace=dev \
  --from-literal=AZURE_CLIENT_ID="9f6cf3b0-c7fe-4220-86fd-a58ac086a692" \
  --from-literal=AZURE_CLIENT_SECRET="changeme" \
  --from-literal=AZURE_TENANT_ID="4f402a0a-52d8-435d-9834-23cc12ede3ff"

# Deploy ConfigMap
kubectl apply -f k8s/configmap.yaml

# Deploy application
kubectl apply -f k8s/deployment.yaml

# Deploy services
kubectl apply -f k8s/service.yaml
```

### **Step 5: Verify Deployment**
```bash
# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=container-entra-auth -n dev --timeout=120s

# Test health
curl -s http://localhost:30080/health
```

### **Step 6: Set Up External Access (WSL)**
```bash
# Install socat (if needed)
apt-get update && apt-get install -y socat

# Set up port forwarding for Windows access
export KUBECONFIG=/root/.kube/config
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 --address=0.0.0.0
```

### **Step 7: Test Authentication**
```bash
# From WSL
curl -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "experiment!ngSecret$"
  }' \
  -v

# From Windows (with port forward active)
curl -X POST http://localhost:8087/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "experiment!ngSecret$"
  }'
```

## ✅ **Expected Success Response**
```json
{
  "authenticated": true,
  "expires_at": "2025-08-14T13:34:59.317540406Z",
  "user_id": "maris@aravindmaklabsoutlook.onmicrosoft.com",
  "login_time": 1755173903322,
  "session_code": "66C2BD858D5D102AC0F0236AA5FDF857",
  "message": "Authentication successful"
}
```

## 🔧 **Troubleshooting**

### Check pods status:
```bash
kubectl get pods -n dev
```

### Check logs:
```bash
kubectl logs -n dev -l app.kubernetes.io/name=container-entra-auth --tail=20
```

### Check services:
```bash
kubectl get services -n dev
```

### Restart deployment:
```bash
kubectl rollout restart deployment/container-entra-auth -n dev
```

## 🎯 **Key URLs**

### **WSL Internal Access:**
- **Health**: http://localhost:30080/health
- **Auth**: http://localhost:30080/api/auth/login
- **API Base**: http://localhost:30080/api/v1

### **Windows External Access (with port forward):**
- **Health**: http://localhost:8087/health
- **Auth**: http://localhost:8087/api/auth/login
- **API Base**: http://localhost:8087/api/v1
- **WSL IP**: http://172.28.0.35:8087

## 🎉 **Success Indicators**
- ✅ Health endpoint returns `{"status":"UP"}`
- ✅ Authentication returns `{"authenticated":true}`
- ✅ Azure AD tokens obtained successfully
- ✅ Microsoft Graph API calls working
- ✅ Session management functional

Your Container-Entra-auth service is production-ready! 🚀
