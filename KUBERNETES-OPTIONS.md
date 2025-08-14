# Kubernetes Options for 12-Factor App Compliance

## 🎯 **12-Factor Principles & Kubernetes Requirements**

| Factor | Requirement | Kind Issues | Better Alternatives |
|--------|-------------|-------------|-------------------|
| **IV. Backing Services** | Network access to Azure AD | ❌ Network isolation | ✅ Full internet access |
| **VI. Processes** | Stateless containers | ✅ Supported | ✅ Supported |
| **VII. Port Binding** | Self-contained services | ✅ Supported | ✅ Supported |
| **VIII. Concurrency** | Horizontal scaling | ✅ Supported | ✅ Better support |
| **IX. Disposability** | Fast startup/shutdown | ✅ Supported | ✅ Better performance |

## 🚀 **Recommended Kubernetes Alternatives**

### **1. Minikube (Best for Development)**

```bash
# Setup
./scripts/setup-minikube.sh

# Deploy
./scripts/create-secret.sh
./scripts/deploy-k8s.sh

# Access
minikube service container-entra-auth-service -n dev
```

**✅ Pros:**
- Full internet connectivity for Azure AD
- Easy to use and well-documented
- Supports all Kubernetes features
- Built-in dashboard and addons
- Perfect for local development

**❌ Cons:**
- Requires more resources than Kind
- Single-node cluster only

### **2. K3s (Best for Production/Edge)**

```bash
# Setup
./scripts/setup-k3s.sh

# Deploy
sudo KUBECONFIG=/etc/rancher/k3s/k3s.yaml ./scripts/create-secret.sh
sudo KUBECONFIG=/etc/rancher/k3s/k3s.yaml ./scripts/deploy-k8s.sh

# Access
http://localhost:30080
```

**✅ Pros:**
- Production-ready and lightweight
- Full internet connectivity
- Perfect for edge computing
- Low resource usage
- Built-in ingress controller

**❌ Cons:**
- Requires sudo for installation
- Less ecosystem tooling than full K8s

### **3. MicroK8s (Best for Ubuntu)**

```bash
# Setup
./scripts/setup-microk8s.sh

# Deploy
./scripts/create-secret.sh
./scripts/deploy-k8s.sh

# Access
http://localhost:30080
```

**✅ Pros:**
- Native Ubuntu integration
- Full Kubernetes compatibility
- Rich addon ecosystem
- Good for CI/CD pipelines

**❌ Cons:**
- Ubuntu/snap specific
- Can be resource intensive

### **4. Cloud Kubernetes (Best for Production)**

```bash
# Configure cloud access first
az aks get-credentials --resource-group myRG --name myCluster
# or
aws eks update-kubeconfig --region us-west-2 --name my-cluster
# or
gcloud container clusters get-credentials my-cluster --zone us-central1-a

# Deploy
./scripts/deploy-cloud-k8s.sh
```

**✅ Pros:**
- True production environment
- Full 12-Factor compliance
- Managed services and scaling
- Enterprise security and monitoring

**❌ Cons:**
- Costs money
- Requires cloud account setup

## 📊 **Comparison Matrix**

| Feature | Kind | Minikube | K3s | MicroK8s | Cloud K8s |
|---------|------|----------|-----|----------|-----------|
| **12-Factor Compliance** | ⚠️ Partial | ✅ Full | ✅ Full | ✅ Full | ✅ Full |
| **Azure AD Access** | ❌ Blocked | ✅ Works | ✅ Works | ✅ Works | ✅ Works |
| **Resource Usage** | 🟢 Low | 🟡 Medium | 🟢 Low | 🟡 Medium | 🔵 Managed |
| **Setup Complexity** | 🟢 Easy | 🟢 Easy | 🟡 Medium | 🟡 Medium | 🔴 Complex |
| **Production Ready** | ❌ No | ❌ No | ✅ Yes | ✅ Yes | ✅ Yes |
| **Cost** | 🟢 Free | 🟢 Free | 🟢 Free | 🟢 Free | 🔴 Paid |

## 🎯 **Recommendations by Use Case**

### **Local Development**
```bash
# Use Minikube for best experience
./scripts/setup-minikube.sh
```

### **Testing/Staging**
```bash
# Use K3s for lightweight production-like environment
./scripts/setup-k3s.sh
```

### **Production**
```bash
# Use Cloud Kubernetes for enterprise features
./scripts/deploy-cloud-k8s.sh
```

### **CI/CD Pipelines**
```bash
# Use MicroK8s for Ubuntu-based CI systems
./scripts/setup-microk8s.sh
```

## 🔧 **Migration from Kind**

If you're currently using Kind, here's how to migrate:

### **1. Stop Kind Cluster**
```bash
kind delete cluster --name container-entra-auth
```

### **2. Choose Alternative**
```bash
# Option A: Minikube
./scripts/setup-minikube.sh

# Option B: K3s
./scripts/setup-k3s.sh

# Option C: MicroK8s
./scripts/setup-microk8s.sh
```

### **3. Deploy Application**
```bash
./scripts/create-secret.sh
./scripts/deploy-k8s.sh
```

### **4. Test Azure AD Authentication**
```bash
curl -X POST http://localhost:8087/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "maris@aravindmaklabsoutlook.onmicrosoft.com",
    "password": "experiment!ngSecret$"
  }'
```

## ✅ **12-Factor Compliance Verification**

With any of these alternatives, your application will be fully 12-Factor compliant:

- ✅ **I. Codebase**: Single repo, multiple deploys
- ✅ **II. Dependencies**: Explicitly declared in Docker
- ✅ **III. Config**: Environment variables via ConfigMaps/Secrets
- ✅ **IV. Backing Services**: Azure AD accessible as attached resource
- ✅ **V. Build/Release/Run**: Separate Docker build and K8s deployment
- ✅ **VI. Processes**: Stateless containers
- ✅ **VII. Port Binding**: Self-contained on port 8080
- ✅ **VIII. Concurrency**: Horizontal scaling ready
- ✅ **IX. Disposability**: Fast startup, graceful shutdown
- ✅ **X. Dev/Prod Parity**: Same containers across environments
- ✅ **XI. Logs**: Stdout/stderr for aggregation
- ✅ **XII. Admin Processes**: kubectl exec for one-off tasks

Choose the option that best fits your environment and requirements! 🎉
