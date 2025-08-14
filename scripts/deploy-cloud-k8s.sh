#!/bin/bash
# Deploy to Cloud Kubernetes (AKS, EKS, GKE) for full 12-Factor compliance

set -e

echo "☁️  Deploying Container-Entra-auth to Cloud Kubernetes"
echo "===================================================="

# Check if kubectl is configured for cloud cluster
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "❌ kubectl is not configured for a Kubernetes cluster."
    echo ""
    echo "📋 Please configure kubectl for your cloud provider:"
    echo ""
    echo "🔷 Azure AKS:"
    echo "  az aks get-credentials --resource-group myResourceGroup --name myAKSCluster"
    echo ""
    echo "🟠 AWS EKS:"
    echo "  aws eks update-kubeconfig --region us-west-2 --name my-cluster"
    echo ""
    echo "🟢 Google GKE:"
    echo "  gcloud container clusters get-credentials my-cluster --zone us-central1-a"
    echo ""
    exit 1
fi

# Verify cluster connectivity
echo "🔍 Verifying cluster connectivity..."
kubectl cluster-info
kubectl get nodes

# Check if image exists locally
if ! docker images container-entra-auth:latest | grep -q container-entra-auth; then
    echo "📦 Building Docker image..."
    ./scripts/build-k8s.sh
fi

# Prompt for container registry
echo ""
echo "📦 Container Registry Configuration:"
echo "===================================="
read -p "Container Registry URL (e.g., myregistry.azurecr.io, 123456789012.dkr.ecr.us-west-2.amazonaws.com): " REGISTRY_URL
read -p "Image name (default: container-entra-auth): " IMAGE_NAME
IMAGE_NAME=${IMAGE_NAME:-container-entra-auth}

FULL_IMAGE_NAME="${REGISTRY_URL}/${IMAGE_NAME}:latest"

echo ""
echo "🚀 Pushing image to registry..."

# Tag and push image
docker tag container-entra-auth:latest $FULL_IMAGE_NAME
docker push $FULL_IMAGE_NAME

echo "✅ Image pushed: $FULL_IMAGE_NAME"

# Update deployment to use cloud image
echo "🔧 Updating deployment configuration..."
sed -i.bak "s|image: container-entra-auth:latest|image: $FULL_IMAGE_NAME|g" k8s/deployment.yaml

# Deploy to cloud cluster
echo "🚀 Deploying to cloud Kubernetes..."

# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create ConfigMap
kubectl apply -f k8s/configmap.yaml

# Create secret (prompt for credentials)
echo ""
echo "🔐 Azure Entra ID Configuration:"
echo "================================"
read -p "Azure Client ID: " AZURE_CLIENT_ID
read -s -p "Azure Client Secret: " AZURE_CLIENT_SECRET
echo ""
read -p "Azure Tenant ID: " AZURE_TENANT_ID

kubectl create secret generic container-entra-auth-secret \
  --namespace=dev \
  --from-literal=AZURE_CLIENT_ID="$AZURE_CLIENT_ID" \
  --from-literal=AZURE_CLIENT_SECRET="$AZURE_CLIENT_SECRET" \
  --from-literal=AZURE_TENANT_ID="$AZURE_TENANT_ID" \
  --dry-run=client -o yaml | kubectl apply -f -

# Deploy application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Wait for deployment to be ready
echo "⏳ Waiting for deployment to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/container-entra-auth -n dev

# Get service information
echo ""
echo "✅ Deployment successful!"
echo ""
echo "📋 Service Information:"
kubectl get services -n dev

# Check if LoadBalancer service is available
EXTERNAL_IP=$(kubectl get service container-entra-auth-service -n dev -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
if [[ -n "$EXTERNAL_IP" ]]; then
    echo ""
    echo "🌐 External Access:"
    echo "  - LoadBalancer IP: $EXTERNAL_IP"
    echo "  - Health Check: http://$EXTERNAL_IP:8080/health"
    echo "  - API Base: http://$EXTERNAL_IP:8080/api/v1"
else
    echo ""
    echo "🔧 Access via Port Forward:"
    echo "  kubectl port-forward -n dev service/container-entra-auth-service 8087:8080"
    echo "  Then access: http://localhost:8087"
fi

# Restore original deployment file
mv k8s/deployment.yaml.bak k8s/deployment.yaml

echo ""
echo "🎉 Container-Entra-auth deployed to cloud Kubernetes!"
echo ""
echo "📋 Management Commands:"
echo "  - Check pods: kubectl get pods -n dev"
echo "  - View logs: kubectl logs -n dev -l app.kubernetes.io/name=container-entra-auth"
echo "  - Scale: kubectl scale deployment/container-entra-auth --replicas=3 -n dev"
echo "  - Update: docker build, push, kubectl rollout restart deployment/container-entra-auth -n dev"
