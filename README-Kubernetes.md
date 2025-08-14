# Container-Entra-auth Kubernetes Deployment

## 🎯 **Tested & Verified Deployment**

This guide contains the **exact tested steps** for deploying Container-Entra-auth with Azure AD authentication on Kubernetes.

**✅ Fully Tested & Working:**
- ✅ Minikube deployment with DNS resolution fixed
- ✅ Azure AD authentication working perfectly
- ✅ Microsoft Graph API integration functional
- ✅ Session management and security working
- ✅ 12-Factor App compliance verified

## 🚀 **Quick Start (Tested Commands)**

### **1. Install Minikube**
```bash
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64
```

### **2. Start Minikube**
```bash
minikube start --driver=none --kubernetes-version=v1.28.0
```

### **3. Build Docker Image**
```bash
./scripts/build-k8s.sh
```

### **4. Deploy to Kubernetes**
```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create Azure secret (with working credentials)
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

### **5. Test Authentication**
```bash
# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=container-entra-auth -n dev --timeout=120s

# Test health
curl -s http://localhost:30080/health

# Test Azure AD authentication
curl -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "experiment!ngSecret$"
  }'
```

**✅ Expected Success Response:**
```json
{
  "authenticated": true,
  "expires_at": "2025-08-14T13:34:59.317540406Z",
  "user_id": "maris@aravindmaklabsoutlook.onmicrosoft.com",
  "session_code": "66C2BD858D5D102AC0F0236AA5FDF857",
  "message": "Authentication successful"
}
```

## 📁 Project Structure

```
├── Dockerfile.k8s                 # Multi-stage Kubernetes-optimized Dockerfile
├── src/main/resources/
│   └── application-k8s.yml        # Kubernetes profile configuration
├── k8s/
│   ├── namespace.yaml             # Dev namespace
│   ├── configmap.yaml             # Non-sensitive configuration
│   ├── secret.yaml                # Azure credentials (template)
│   ├── deployment.yaml            # Application deployment
│   └── service.yaml               # ClusterIP and NodePort services
└── scripts/
    ├── build-k8s.sh              # Build Docker image
    ├── deploy-k8s.sh              # Deploy to Kubernetes
    └── create-secret.sh           # Create Azure credentials secret
```

## 🔧 Configuration

### ConfigMap (Non-sensitive)
- Application name and ports
- API base path and CORS settings
- Logging levels
- Azure redirect URI

### Secret (Sensitive)
- Azure Client ID
- Azure Client Secret  
- Azure Tenant ID

### Environment Variables

| Variable | Source | Description |
|----------|--------|-------------|
| `AZURE_CLIENT_ID` | Secret | Azure application client ID |
| `AZURE_CLIENT_SECRET` | Secret | Azure application client secret |
| `AZURE_TENANT_ID` | Secret | Azure tenant ID |
| `AZURE_REDIRECT_URI` | ConfigMap | OAuth2 redirect URI |
| `API_BASE_PATH` | ConfigMap | API base path (default: /api/v1) |
| `CORS_ALLOWED_ORIGINS` | ConfigMap | Allowed CORS origins |
| `LOG_LEVEL_ROOT` | ConfigMap | Root logging level |

## 🏥 Health Checks

The application exposes health check endpoints for Kubernetes:

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **General Health**: `/actuator/health`

## 📊 Monitoring

```bash
# Check deployment status
kubectl get all -n dev -l app.kubernetes.io/name=container-entra-auth

# View pod logs
kubectl logs -n dev -l app.kubernetes.io/name=container-entra-auth -f

# Check health
kubectl port-forward -n dev service/container-entra-auth-service 8080:8080
curl http://localhost:8080/actuator/health
```

## 🔒 Security Features

- **Non-root user**: Container runs as user ID 1001
- **Read-only filesystem**: Root filesystem is read-only
- **No privilege escalation**: Security context prevents privilege escalation
- **Secrets management**: Azure credentials stored in Kubernetes Secrets
- **Network policies**: Ready for network policy implementation

## 🛠️ Development Workflow

### Local Development with Docker

```bash
# Build and run locally
./scripts/build-k8s.sh
docker run -p 8080:8080 \
  -e AZURE_CLIENT_ID=your-client-id \
  -e AZURE_CLIENT_SECRET=your-client-secret \
  -e AZURE_TENANT_ID=your-tenant-id \
  -e AZURE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/azure \
  container-entra-auth:latest
```

### Update Configuration

```bash
# Update ConfigMap
kubectl apply -f k8s/configmap.yaml

# Update Secret
./scripts/create-secret.sh

# Restart deployment to pick up changes
kubectl rollout restart deployment/container-entra-auth -n dev
```

### Scaling

```bash
# Scale up/down
kubectl scale deployment/container-entra-auth --replicas=3 -n dev

# Check scaling status
kubectl get deployment/container-entra-auth -n dev
```

## 🐛 Troubleshooting

### Common Issues

1. **Pod not starting**
   ```bash
   kubectl describe pod -n dev -l app.kubernetes.io/name=container-entra-auth
   kubectl logs -n dev -l app.kubernetes.io/name=container-entra-auth
   ```

2. **Secret not found**
   ```bash
   kubectl get secret container-entra-auth-secret -n dev
   ./scripts/create-secret.sh
   ```

3. **Health check failures**
   ```bash
   kubectl port-forward -n dev service/container-entra-auth-service 8080:8080
   curl http://localhost:8080/actuator/health
   ```

### Useful Commands

```bash
# Get all resources
kubectl get all -n dev

# Describe deployment
kubectl describe deployment/container-entra-auth -n dev

# Execute into pod
kubectl exec -it -n dev deployment/container-entra-auth -- /bin/sh

# View events
kubectl get events -n dev --sort-by=.metadata.creationTimestamp
```

## 🌟 12-Factor App Compliance

✅ **I. Codebase**: Single codebase, multiple deploys  
✅ **II. Dependencies**: Explicitly declared in Dockerfile  
✅ **III. Config**: Stored in environment via ConfigMaps/Secrets  
✅ **IV. Backing services**: Azure AD as attached resource  
✅ **V. Build, release, run**: Separate Docker build and K8s deployment  
✅ **VI. Processes**: Stateless containers  
✅ **VII. Port binding**: Self-contained service on port 8080  
✅ **VIII. Concurrency**: Horizontal scaling via replicas  
✅ **IX. Disposability**: Fast startup, graceful shutdown  
✅ **X. Dev/prod parity**: Same containers across environments  
✅ **XI. Logs**: Stdout/stderr for Kubernetes log aggregation  
✅ **XII. Admin processes**: kubectl exec for one-off tasks  

## 🐳 Local Docker Development

For local development without Kubernetes:

```bash
# Build the image
docker build -f Dockerfile.k8s -t container-entra-auth:latest .

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=k8s \
  -e AZURE_CLIENT_ID=your-client-id \
  -e AZURE_CLIENT_SECRET=your-client-secret \
  -e AZURE_TENANT_ID=your-tenant-id \
  -e AZURE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/azure \
  -e API_BASE_PATH=/api/v1 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  container-entra-auth:latest

# Access the application
curl http://localhost:8080/actuator/health
```

Your Container-Entra-auth service is now fully containerized and ready for production Kubernetes deployment! 🎉
