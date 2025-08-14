#!/bin/bash
# Deploy Container-Entra-auth to Kubernetes

set -e

echo "🚀 Deploying Container-Entra-auth to Kubernetes"
echo "==============================================="

# Get the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to project root directory
cd "${PROJECT_ROOT}"

echo "📁 Project root: ${PROJECT_ROOT}"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed or not in PATH"
    exit 1
fi

# Check if we can connect to Kubernetes cluster
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster"
    echo "💡 Make sure your kubeconfig is set up correctly"
    exit 1
fi

echo "✅ Connected to Kubernetes cluster"

# Deploy in order
echo "📦 Creating namespace..."
kubectl apply -f k8s/namespace.yaml

echo "📝 Creating ConfigMap..."
kubectl apply -f k8s/configmap.yaml

echo "🔐 Creating Secret (template)..."
echo "⚠️  WARNING: You need to update k8s/secret.yaml with your actual Azure credentials"
echo "   Or create the secret manually with:"
echo "   kubectl create secret generic container-entra-auth-secret \\"
echo "     --namespace=dev \\"
echo "     --from-literal=AZURE_CLIENT_ID=your-client-id \\"
echo "     --from-literal=AZURE_CLIENT_SECRET=your-client-secret \\"
echo "     --from-literal=AZURE_TENANT_ID=your-tenant-id"
echo ""
read -p "Press Enter to continue with template secret (you'll need to update it later)..."
kubectl apply -f k8s/secret.yaml

echo "🚀 Creating Deployment..."
kubectl apply -f k8s/deployment.yaml

echo "🌐 Creating Service..."
kubectl apply -f k8s/service.yaml

echo "✅ Deployment completed!"
echo ""
echo "📊 Checking deployment status..."
kubectl get all -n dev -l app.kubernetes.io/name=container-entra-auth

echo ""
echo "🔍 To monitor the deployment:"
echo "kubectl get pods -n dev -w"
echo ""
echo "📋 To view logs:"
echo "kubectl logs -n dev -l app.kubernetes.io/name=container-entra-auth -f"
echo ""
echo "🌐 To access the application:"
echo "kubectl port-forward -n dev service/container-entra-auth-service 8080:8080"
echo "Then visit: http://localhost:8080"
