#!/bin/bash
# Setup K3s cluster for 12-Factor App compliance

set -e

echo "🐄 Setting up K3s for Container-Entra-auth (12-Factor Compliant)"
echo "=============================================================="

# Check if running as root or with sudo
if [[ $EUID -eq 0 ]]; then
    echo "⚠️  Running as root. K3s will be installed system-wide."
else
    echo "📋 K3s requires sudo privileges for installation."
fi

# Install K3s
echo "📦 Installing K3s..."
curl -sfL https://get.k3s.io | sh -s - \
  --write-kubeconfig-mode 644 \
  --disable traefik \
  --disable servicelb

# Wait for K3s to be ready
echo "⏳ Waiting for K3s to be ready..."
sleep 30

# Set up kubectl configuration
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
echo "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml" >> ~/.bashrc

# Wait for node to be ready
kubectl wait --for=condition=Ready nodes --all --timeout=300s

# Install NGINX Ingress Controller
echo "🌐 Installing NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/baremetal/deploy.yaml

# Wait for ingress controller to be ready
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

# Import Docker image to K3s
echo "📦 Importing Docker image to K3s..."
if docker images container-entra-auth:latest | grep -q container-entra-auth; then
    docker save container-entra-auth:latest | sudo k3s ctr images import -
else
    echo "⚠️  Docker image not found. Building first..."
    ./scripts/build-k8s.sh
    docker save container-entra-auth:latest | sudo k3s ctr images import -
fi

# Test cluster networking
echo "🌐 Testing cluster networking..."
kubectl run test-dns --image=busybox --rm -it --restart=Never -- nslookup google.com || true

echo "✅ K3s cluster is ready!"
echo ""
echo "📋 Cluster Information:"
kubectl cluster-info
echo ""
echo "📋 Node Information:"
kubectl get nodes -o wide
echo ""
echo "📋 Next steps:"
echo "1. Create Azure secret: sudo KUBECONFIG=/etc/rancher/k3s/k3s.yaml ./scripts/create-secret.sh"
echo "2. Deploy application: sudo KUBECONFIG=/etc/rancher/k3s/k3s.yaml ./scripts/deploy-k8s.sh"
echo "3. Access via NodePort: http://localhost:30080"
echo ""
echo "🔧 Useful commands:"
echo "  - Get pods: kubectl get pods -n dev"
echo "  - Get services: kubectl get services -n dev"
echo "  - Port forward: kubectl port-forward -n dev service/container-entra-auth-service 8087:8080"
echo "  - Uninstall: /usr/local/bin/k3s-uninstall.sh"
