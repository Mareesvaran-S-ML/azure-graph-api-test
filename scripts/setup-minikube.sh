#!/bin/bash
# Setup Minikube cluster with proper networking for 12-Factor App compliance

set -e

echo "🚀 Setting up Minikube for Container-Entra-auth (12-Factor Compliant)"
echo "=================================================================="

# Check if Minikube is installed
if ! command -v minikube &> /dev/null; then
    echo "📦 Installing Minikube..."
    curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
    sudo install minikube-linux-amd64 /usr/local/bin/minikube
    rm minikube-linux-amd64
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "📦 Installing kubectl..."
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    sudo install kubectl /usr/local/bin/kubectl
    rm kubectl
fi

# Stop existing Minikube if running
if minikube status >/dev/null 2>&1; then
    echo "🛑 Stopping existing Minikube cluster..."
    minikube stop
    minikube delete
fi

echo "🚀 Starting Minikube with proper networking..."

# Start Minikube
if [[ $EUID -eq 0 ]]; then
    echo "⚠️  Running as root, using none driver..."
    minikube start \
      --driver=none \
      --kubernetes-version=v1.28.0 \
      --extra-config=kubelet.resolv-conf=/run/systemd/resolve/resolv.conf
else
    echo "👤 Running as user, using docker driver..."
    minikube start \
      --driver=docker \
      --cpus=2 \
      --memory=4096 \
      --disk-size=20g \
      --kubernetes-version=v1.28.0
fi

# Wait for cluster to be ready
echo "⏳ Waiting for cluster to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=300s

# Enable addons
echo "🔧 Enabling Kubernetes addons..."
minikube addons enable ingress
minikube addons enable metrics-server

# Configure Docker environment
if [[ $EUID -ne 0 ]]; then
    echo "🐳 Configuring Docker environment..."
    eval $(minikube docker-env)
fi

# Build image in Minikube
echo "📦 Building Docker image in Minikube..."
./scripts/build-k8s.sh

# Test cluster networking
echo "🌐 Testing cluster networking..."
kubectl run test-dns --image=busybox --rm -it --restart=Never -- nslookup google.com || true

echo "✅ Minikube cluster is ready!"
echo ""
echo "📋 Cluster Information:"
minikube status
echo ""
if [[ $EUID -ne 0 ]]; then
    echo "🌐 Cluster IP: $(minikube ip)"
    echo "📊 Dashboard: minikube dashboard"
fi
echo ""
echo "📋 Next steps:"
echo "1. Create Azure secret: ./scripts/create-secret.sh"
echo "2. Deploy application: ./scripts/deploy-k8s.sh"
if [[ $EUID -ne 0 ]]; then
    echo "3. Access via: minikube service container-entra-auth-service -n dev"
else
    echo "3. Access via: http://localhost:30080"
fi
echo ""
echo "🔧 Useful commands:"
if [[ $EUID -ne 0 ]]; then
    echo "  - Get service URL: minikube service container-entra-auth-service -n dev --url"
    echo "  - Open dashboard: minikube dashboard"
    echo "  - SSH to node: minikube ssh"
fi
echo "  - Stop cluster: minikube stop"
echo "  - Delete cluster: minikube delete"
