#!/bin/bash

# Build and Deploy script for Container-Login-Service

set -e

echo "🚀 Container-Login-Service Build and Deploy"
echo "==========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SERVICE_NAME="container-login-service"
IMAGE_NAME="container-login-service"
IMAGE_TAG="${1:-latest}"
NAMESPACE="dev"

echo -e "${BLUE}📦 Building and deploying: ${IMAGE_NAME}:${IMAGE_TAG}${NC}"

# Check if Minikube is running
if ! minikube status | grep -q "Running"; then
    echo -e "${RED}❌ Minikube is not running. Please start it with: minikube start${NC}"
    exit 1
fi

# Set up Docker environment
echo -e "${YELLOW}🐳 Setting up Docker environment...${NC}"
DRIVER=$(minikube config get driver 2>/dev/null || echo "none")
if [ "$DRIVER" = "none" ]; then
    echo -e "${YELLOW}🐳 Using host Docker (none driver)...${NC}"
else
    eval $(minikube docker-env)
fi
echo -e "${GREEN}✅ Docker environment configured${NC}"

# Build JAR
echo -e "${YELLOW}🔨 Building JAR...${NC}"
./mvnw clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ JAR built successfully${NC}"
else
    echo -e "${RED}❌ JAR build failed${NC}"
    exit 1
fi

# Build Docker image
echo -e "${YELLOW}🐳 Building Docker image...${NC}"
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Docker image built successfully${NC}"
else
    echo -e "${RED}❌ Docker build failed${NC}"
    exit 1
fi

# Load image into Minikube
echo -e "${YELLOW}📦 Loading image into Minikube...${NC}"
if minikube status >/dev/null 2>&1; then
    echo "Loading ${IMAGE_NAME}:${IMAGE_TAG} into Minikube..."
    minikube image load "${IMAGE_NAME}:${IMAGE_TAG}"


    echo -e "${YELLOW}🔍 Verifying image in Minikube...${NC}"
    if minikube image ls | grep -q "${IMAGE_NAME}:${IMAGE_TAG}"; then
        echo -e "${GREEN}✅ Image verified in Minikube successfully${NC}"
    else
        echo -e "${RED}❌ Image not found in Minikube after loading${NC}"
        echo "Available images in Minikube:"
        minikube image ls | grep "${IMAGE_NAME}" || echo "No ${IMAGE_NAME} images found"
        exit 1
    fi


    echo -e "${YELLOW}📦 Checking for Azure Graph API service image...${NC}"
    if docker images | grep -q "container-entra-auth.*latest"; then
        echo "Loading container-entra-auth:latest into Minikube..."
        minikube image load "container-entra-auth:latest"
        echo -e "${GREEN}✅ Azure Graph API service image loaded${NC}"
    else
        echo -e "${YELLOW}⚠️  Azure Graph API service image not found locally${NC}"
    fi
else
    echo -e "${RED}❌ Minikube not running! Cannot load image.${NC}"
    echo "Please start Minikube first with: minikube start"
    exit 1
fi

# Create namespace
echo -e "${YELLOW}📦 Creating namespace: ${NAMESPACE}${NC}"
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# Check for shared secret
echo -e "${YELLOW}🔐 Checking for shared secret...${NC}"
if kubectl get secret container-entra-auth-secret -n ${NAMESPACE} >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Found shared secret 'container-entra-auth-secret'${NC}"
else
    echo -e "${RED}⚠️  WARNING: Shared secret 'container-entra-auth-secret' not found!${NC}"
    echo "   This secret should be created by the azure-graph-api-test service."
    echo "   Please run the following from the azure-graph-api-test directory:"
    echo "   ./scripts/create-secret.sh"
    echo ""
    echo "   Or create it manually with:"
    echo "   kubectl create secret generic container-entra-auth-secret \\"
    echo "     --namespace=dev \\"
    echo "     --from-literal=AZURE_CLIENT_ID=your-client-id \\"
    echo "     --from-literal=AZURE_CLIENT_SECRET=your-client-secret \\"
    echo "     --from-literal=AZURE_TENANT_ID=your-tenant-id"
    echo ""
    read -p "Press Enter to continue (deployment may fail without the secret)..."
fi

# Step 6: Apply Kubernetes manifests
echo -e "${YELLOW}⚙️  Applying Kubernetes manifests...${NC}"
kubectl apply -f k8s/ -n ${NAMESPACE}

# Step 6.1: Ensure single replica (permanent configuration)
echo -e "${YELLOW}🔧 Ensuring single replica configuration...${NC}"
kubectl scale deployment ${SERVICE_NAME} --replicas=1 -n ${NAMESPACE}

# Step 7: Wait for deployment to be ready
echo -e "${YELLOW}⏳ Waiting for deployment to be ready...${NC}"
kubectl wait --for=condition=available deployment/${SERVICE_NAME} -n ${NAMESPACE} --timeout=300s

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Deployment is ready${NC}"
else
    echo -e "${RED}❌ Deployment failed or timed out${NC}"
    exit 1
fi

# Step 8: Show status
echo -e "${BLUE}📋 Deployment Status:${NC}"
kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=${SERVICE_NAME}
kubectl get services -n ${NAMESPACE} -l app.kubernetes.io/name=${SERVICE_NAME}

# Step 9: Get access information
NODEPORT=$(kubectl get service ${SERVICE_NAME}-nodeport -n ${NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "localhost")

echo ""
echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
echo ""
echo -e "${BLUE}🎯 Access Information:${NC}"
echo "  Service Name: ${SERVICE_NAME}"
echo "  Namespace: ${NAMESPACE}"
echo "  NodePort: ${NODEPORT}"
echo "  Minikube IP: ${MINIKUBE_IP}"
echo ""
echo -e "${BLUE}🌐 Access URLs:${NC}"
if [ "$NODEPORT" != "N/A" ]; then
    echo "  External: http://${MINIKUBE_IP}:${NODEPORT}"
    echo "  Health:   http://${MINIKUBE_IP}:${NODEPORT}/actuator/health"
    echo "  Users:    http://${MINIKUBE_IP}:${NODEPORT}/users?page=0&size=10"
fi
echo "  Internal: http://${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local:8080"
echo ""
echo -e "${BLUE}🔧 Useful Commands:${NC}"
echo "  View logs:    kubectl logs -f deployment/${SERVICE_NAME} -n ${NAMESPACE}"
echo "  Port forward: kubectl port-forward service/${SERVICE_NAME} 8080:8080 -n ${NAMESPACE}"
echo "  Scale:        kubectl scale deployment/${SERVICE_NAME} --replicas=3 -n ${NAMESPACE}"
echo "  Access via Minikube: minikube service ${SERVICE_NAME}-nodeport -n ${NAMESPACE}"
echo "  Delete:       kubectl delete -f k8s/ -n ${NAMESPACE}"
