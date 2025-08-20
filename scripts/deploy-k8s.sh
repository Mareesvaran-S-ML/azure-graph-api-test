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

echo "🔐 Checking Secret..."
if kubectl get secret container-entra-auth-secret -n dev &>/dev/null; then
    echo "✅ Secret already exists, skipping creation"
else
    echo "📄 Secret not found, creating from .env file..."
    if [[ -f ".env" ]]; then
        ./scripts/create-secret.sh
    else
        echo "⚠️  WARNING: .env file not found!"
        echo "   Please create a .env file with your Azure credentials or run deploy-azure-services.sh"
        echo "   Continuing without secret creation..."
    fi
fi

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
echo "kubectl port-forward -n dev service/container-entra-auth-service 8087:8080 --address=0.0.0.0"
echo "Then visit: http://localhost:8087"
