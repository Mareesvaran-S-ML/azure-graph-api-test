# Container-Entra-auth

## 🎯 **Azure AD Authentication Service**

A production-ready Spring Boot application that provides Azure Entra ID (Azure AD) authentication services, containerized and deployed on Kubernetes with full 12-Factor App compliance.

## ✅ **Tested & Verified**

This project has been **fully tested and verified** to work with:
- ✅ **Azure AD Authentication**: Full OAuth2 flow working
- ✅ **Microsoft Graph API**: User profile retrieval functional  
- ✅ **Kubernetes Deployment**: Minikube deployment successful
- ✅ **DNS Resolution**: External connectivity fixed
- ✅ **Session Management**: Secure session handling
- ✅ **12-Factor Compliance**: All principles implemented

## 🚀 **Quick Start**

### **Option 1: Kubernetes (Recommended)**
```bash
# Install Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64

# Start cluster and deploy
minikube start --driver=none --kubernetes-version=v1.28.0
./scripts/build-k8s.sh
kubectl apply -f k8s/namespace.yaml
kubectl create secret generic container-entra-auth-secret --namespace=dev \
  --from-literal=AZURE_CLIENT_ID="9f6cf3b0-c7fe-4220-86fd-a58ac086a692" \
  --from-literal=AZURE_CLIENT_SECRET="changeme" \
  --from-literal=AZURE_TENANT_ID="4f402a0a-52d8-435d-9834-23cc12ede3ff"
kubectl apply -f k8s/configmap.yaml k8s/deployment.yaml k8s/service.yaml

# Test authentication
curl -X POST http://localhost:30080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "maris@aravindmaklabsoutlook.onmicrosoft.com", "password": "experiment!ngSecret$"}'
```

### **Option 2: Docker**
```bash
./scripts/run-docker.sh
```

## 📚 **Documentation**

| Document | Description |
|----------|-------------|
| **[QUICK-DEPLOY.md](QUICK-DEPLOY.md)** | ⚡ Copy-paste deployment commands |
| **[DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md)** | 📖 Complete deployment guide |
| **[README-Kubernetes.md](README-Kubernetes.md)** | ☸️ Kubernetes deployment details |
| **[KUBERNETES-OPTIONS.md](KUBERNETES-OPTIONS.md)** | 🔧 Alternative K8s platforms |

## 🎯 **Key Features**

- **🔐 Azure AD Integration**: Full OAuth2 authentication flow
- **📊 Microsoft Graph API**: User profile and permissions retrieval
- **🔒 Session Management**: Secure session handling with CSRF protection
- **☸️ Kubernetes Ready**: Production-ready manifests with health checks
- **📏 12-Factor Compliant**: All 12 factors implemented
- **🛡️ Security**: Non-root containers, read-only filesystem, secure headers
- **📈 Scalable**: Horizontal pod autoscaling ready
- **🔍 Monitoring**: Health endpoints and comprehensive logging

## 🧪 **API Endpoints**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/api/auth/login` | POST | Azure AD authentication |
| `/api/auth/user` | GET | Get user profile |
| `/api/auth/status` | GET | Authentication status |
| `/api/auth/permissions` | GET | User permissions |

## ✅ **Success Response**

```json
{
  "authenticated": true,
  "expires_at": "2025-08-14T13:34:59.317540406Z",
  "user_id": "maris@aravindmaklabsoutlook.onmicrosoft.com",
  "session_code": "66C2BD858D5D102AC0F0236AA5FDF857",
  "message": "Authentication successful"
}
```

## 🏗️ **Architecture**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   User/Client   │───▶│  Kubernetes      │───▶│   Azure AD      │
│                 │    │  (Minikube)      │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │ Microsoft Graph  │
                       │      API         │
                       └──────────────────┘
```

## 🔧 **Configuration**

| Variable | Value | Description |
|----------|-------|-------------|
| `AZURE_CLIENT_ID` | `9f6cf3b0-c7fe-4220-86fd-a58ac086a692` | Azure App Registration Client ID |
| `AZURE_CLIENT_SECRET` | `changeme` | Azure App Registration Secret |
| `AZURE_TENANT_ID` | `4f402a0a-52d8-435d-9834-23cc12ede3ff` | Azure Tenant ID |

## 🎉 **Production Ready**

Your Container-Entra-auth service is fully tested and production-ready! 🚀

- ✅ **Tested Authentication**: Azure AD OAuth2 flow working
- ✅ **Tested Deployment**: Kubernetes manifests verified
- ✅ **Tested Networking**: DNS resolution and external connectivity
- ✅ **Tested Security**: Session management and CSRF protection
- ✅ **Tested Scalability**: Horizontal scaling ready
