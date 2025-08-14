# Container-Entra-auth Deployment Guide

## 🚀 **Tested Deployment Steps for New Machine**

This guide contains the **exact steps that have been tested and verified** to work for deploying Container-Entra-auth with Azure AD authentication.

### **Prerequisites Installation**

```bash
# Install Docker (if not already installed)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
# Logout and login again

# Install kubectl (if not already installed)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install Java 17 (for development - optional)
sudo apt update && sudo apt install openjdk-17-jdk maven -y
```

### **Project Setup**

```bash
# Clone repository
git clone <your-repository-url>
cd azure-graph-api-test

# Make scripts executable
chmod +x scripts/*.sh
```

## ☸️ **Minikube Deployment (Tested & Verified)**

**✅ These are the exact steps that have been tested and work perfectly:**

### **Step 1: Install Minikube**

```bash
# Download and install Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64
```

### **Step 2: Start Minikube Cluster**

```bash
# Start Minikube with none driver (for root) or docker driver (for user)
minikube start --driver=none --kubernetes-version=v1.28.0

# If you get KUBECONFIG issues, run:
unset KUBECONFIG && minikube start --driver=none --kubernetes-version=v1.28.0
```

### **Step 3: Build Docker Image**

```bash
# Build the application Docker image
./scripts/build-k8s.sh
```

### **Step 4: Create Kubernetes Resources**

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create Azure secret with your credentials
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
# Check pods are running
kubectl get pods -n dev

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=container-entra-auth -n dev --timeout=120s

# Check services
kubectl get services -n dev

# Test health endpoint
curl -s http://localhost:30080/health
```

### **Step 6: Test Azure AD Authentication**

```bash
# Test the authentication endpoint
curl -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "experiment!ngSecret$"
  }' \
  -v
```

**✅ Expected Success Response:**
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

## 🐳 **Alternative: Docker Deployment**

**For simple testing without Kubernetes:**

```bash
# Build and run with Docker
./scripts/run-docker.sh

# Or manual Docker run
docker run -d --name container-entra-auth-prod --network host \
  -e AZURE_CLIENT_ID="9f6cf3b0-c7fe-4220-86fd-a58ac086a692" \
  -e AZURE_CLIENT_SECRET="changeme" \
  -e AZURE_TENANT_ID="4f402a0a-52d8-435d-9834-23cc12ede3ff" \
  -e SERVER_PORT=8087 \
  container-entra-auth:latest

# Test with Docker
curl -X POST http://localhost:8087/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "maris@aravindmaklabsoutlook.onmicrosoft.com", "password": "experiment!ngSecret$"}'
```

## 🔧 **Configuration**

### **Required Azure Credentials**

| Variable | Value (Example) | Description |
|----------|-----------------|-------------|
| `AZURE_CLIENT_ID` | `9f6cf3b0-c7fe-4220-86fd-a58ac086a692` | Azure App Registration Client ID |
| `AZURE_CLIENT_SECRET` | `changeme` | Azure App Registration Secret |
| `AZURE_TENANT_ID` | `4f402a0a-52d8-435d-9834-23cc12ede3ff` | Azure Tenant ID |

### **Application Configuration**

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Application port (NodePort: 30080) |
| `API_BASE_PATH` | `/api/v1` | API base path |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:8080` | CORS origins |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |

## 🧪 **Testing & Verification**

### **Health Check**

```bash
# Minikube deployment
curl http://localhost:30080/health

# Docker deployment
curl http://localhost:8087/health
```

**✅ Expected Response:**
```json
{"service":"container-entra-auth","status":"UP","timestamp":1755173889898}
```

### **Authentication Test**

```bash
# Test Azure AD authentication
curl -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "experiment!ngSecret$"
  }' \
  -v
```

**✅ Success Response:**
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

### **Follow-up API Tests**

```bash
# Save session cookie
curl -c cookies.txt -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "maris@aravindmaklabsoutlook.onmicrosoft.com", "password": "experiment!ngSecret$"}'

# Test authenticated endpoints
curl -b cookies.txt http://localhost:30080/api/auth/user
curl -b cookies.txt http://localhost:30080/api/auth/status
curl -b cookies.txt http://localhost:30080/api/auth/permissions
```

## 🌐 **WSL External Access**

### **Port Forward for Windows Access**

```bash
# Set up port forwarding for external access
export KUBECONFIG=/root/.kube/config
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 --address=0.0.0.0
```

**✅ Access URLs:**
- **From Windows**: http://localhost:8087
- **WSL IP**: http://172.28.0.35:8087
- **Health Check**: http://localhost:8087/health

### **Test from Windows**

```bash
# Health check from Windows Command Prompt
curl http://localhost:8087/health

# Authentication from Windows
curl -X POST http://localhost:8087/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"maris@aravindmaklabsoutlook.onmicrosoft.com\", \"password\": \"experiment!ngSecret$\"}"
```

**📋 See [WSL-ACCESS-GUIDE.md](WSL-ACCESS-GUIDE.md) for complete external access setup.**

## 🔍 Troubleshooting

### Docker Issues

```bash
# Check container logs
docker logs container-entra-auth-prod

# Check container status
docker ps -a | grep container-entra-auth

# Restart container
docker restart container-entra-auth-prod
```

### Kubernetes Issues

```bash
# Check pod status
kubectl get pods -n dev

# Check pod logs
kubectl logs -n dev -l app.kubernetes.io/name=container-entra-auth

# Check services
kubectl get services -n dev

# Port forward for testing
kubectl port-forward -n dev service/container-entra-auth-service 8087:8080
```

### Network Issues

- **Docker**: Use `--network host` for Azure AD access
- **Kubernetes**: Ensure cluster has internet connectivity
- **Firewall**: Check ports 8087, 443 (HTTPS to Azure)

## 📋 Management Commands

### Docker

```bash
# Start
docker start container-entra-auth-prod

# Stop
docker stop container-entra-auth-prod

# Remove
docker rm -f container-entra-auth-prod

# Update (rebuild and redeploy)
./scripts/build-k8s.sh && docker rm -f container-entra-auth-prod && ./scripts/run-docker.sh
```

### Kubernetes

```bash
# Scale
kubectl scale deployment/container-entra-auth --replicas=3 -n dev

# Update
./scripts/build-k8s.sh
kind load docker-image container-entra-auth:latest --name container-entra-auth
kubectl rollout restart deployment/container-entra-auth -n dev

# Delete
kubectl delete -f k8s/
```

## 🎯 Production Considerations

### Security
- Use Kubernetes Secrets for credentials
- Enable HTTPS/TLS
- Configure proper CORS origins
- Use non-root container user

### Monitoring
- Enable application metrics
- Set up log aggregation
- Configure health checks
- Monitor Azure AD token usage

### Scaling
- Use horizontal pod autoscaling
- Configure resource limits
- Set up load balancing
- Monitor performance metrics

## ✅ Success Indicators

- ✅ Health endpoint returns `{"status":"UP"}`
- ✅ Authentication returns `{"authenticated":true}`
- ✅ Azure AD tokens obtained successfully
- ✅ Microsoft Graph API calls working
- ✅ Session management functional

Your Container-Entra-auth service is production-ready! 🎉
