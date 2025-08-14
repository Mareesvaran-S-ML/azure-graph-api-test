#!/bin/bash
# Setup MicroK8s cluster for 12-Factor App compliance

set -e

echo "🔷 Setting up MicroK8s for Container-Entra-auth (12-Factor Compliant)"
echo "=================================================================="

# Install MicroK8s via snap
echo "📦 Installing MicroK8s..."
sudo snap install microk8s --classic

# Add user to microk8s group
sudo usermod -a -G microk8s $USER
sudo chown -f -R $USER ~/.kube

# Wait for MicroK8s to be ready
echo "⏳ Waiting for MicroK8s to be ready..."
sudo microk8s status --wait-ready

# Enable essential addons
echo "🔧 Enabling MicroK8s addons..."
sudo microk8s enable dns
sudo microk8s enable storage
sudo microk8s enable ingress
sudo microk8s enable metrics-server
sudo microk8s enable registry

# Set up kubectl alias
echo "🔧 Setting up kubectl..."
sudo snap alias microk8s.kubectl kubectl
mkdir -p ~/.kube
sudo microk8s config > ~/.kube/config

# Wait for all pods to be ready
echo "⏳ Waiting for system pods to be ready..."
kubectl wait --for=condition=Ready pods --all -n kube-system --timeout=300s

# Build and import Docker image
echo "📦 Building and importing Docker image..."
if ! docker images container-entra-auth:latest | grep -q container-entra-auth; then
    ./scripts/build-k8s.sh
fi

# Import image to MicroK8s registry
docker save container-entra-auth:latest | sudo microk8s ctr image import -

# Test cluster networking
echo "🌐 Testing cluster networking..."
kubectl run test-dns --image=busybox --rm -it --restart=Never -- nslookup google.com || true

echo "✅ MicroK8s cluster is ready!"
echo ""
echo "📋 Cluster Information:"
kubectl cluster-info
echo ""
echo "📋 Enabled Addons:"
sudo microk8s status
echo ""
echo "📋 Next steps:"
echo "1. Create Azure secret: ./scripts/create-secret.sh"
echo "2. Deploy application: ./scripts/deploy-k8s.sh"
echo "3. Access via NodePort: http://localhost:30080"
echo ""
echo "🔧 Useful commands:"
echo "  - Get pods: kubectl get pods -n dev"
echo "  - Get services: kubectl get services -n dev"
echo "  - Port forward: kubectl port-forward -n dev service/container-entra-auth-service 8087:8080"
echo "  - Stop: sudo microk8s stop"
echo "  - Start: sudo microk8s start"
echo "  - Remove: sudo snap remove microk8s"
