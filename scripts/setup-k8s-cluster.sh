#!/bin/bash
# Setup Kubernetes cluster with proper networking for Azure AD access

set -e

echo "☸️  Setting up Kubernetes Cluster for Container-Entra-auth"
echo "========================================================"

# Check if Kind is installed
if ! command -v kind &> /dev/null; then
    echo "❌ Kind is not installed. Please install Kind first:"
    echo "curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64"
    echo "chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed. Please install kubectl first."
    exit 1
fi

# Delete existing cluster if it exists
if kind get clusters | grep -q container-entra-auth; then
    echo "🗑️  Deleting existing cluster..."
    kind delete cluster --name container-entra-auth
fi

echo "🚀 Creating Kind cluster with network access..."

# Create Kind cluster configuration for better networking
cat > kind-cluster-config.yaml << EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: container-entra-auth
networking:
  # Use default CNI for better external connectivity
  disableDefaultCNI: false
  kubeProxyMode: "iptables"
nodes:
- role: control-plane
  # Port mappings for NodePort services
  extraPortMappings:
  - containerPort: 30080
    hostPort: 30080
    protocol: TCP
EOF

# Create cluster
kind create cluster --config kind-cluster-config.yaml

# Wait for cluster to be ready
echo "⏳ Waiting for cluster to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=300s

# Load Docker image into Kind cluster
echo "📦 Loading Docker image into Kind cluster..."
kind load docker-image container-entra-auth:latest --name container-entra-auth

echo "✅ Kubernetes cluster is ready!"
echo ""
echo "📋 Next steps:"
echo "1. Create Azure secret: ./scripts/create-secret.sh"
echo "2. Deploy application: ./scripts/deploy-k8s.sh"
echo "3. Access via NodePort: http://localhost:30080"

# Clean up
rm -f kind-cluster-config.yaml
