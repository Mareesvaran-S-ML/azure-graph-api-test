#!/bin/bash
# Build script for Container-Entra-auth Kubernetes deployment

set -e

echo "🔨 Building Container-Entra-auth for Kubernetes"
echo "==============================================="

# Get the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Configuration
IMAGE_NAME="container-entra-auth"
IMAGE_TAG="${1:-latest}"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

echo "📦 Building Docker image: ${FULL_IMAGE_NAME}"
echo "📁 Project root: ${PROJECT_ROOT}"

# Change to project root directory
cd "${PROJECT_ROOT}"

# Build JAR locally
echo "🔨 Building JAR file locally..."
if [ -f "./mvnw" ]; then
    echo "Using Maven wrapper..."
    chmod +x ./mvnw
    ./mvnw clean package -DskipTests
else
    echo "Using system Maven..."
    mvn clean package -DskipTests
fi

# Verify JAR exists
if [ ! -f "target/azure-graph-api-0.0.1-SNAPSHOT.jar" ]; then
    echo "❌ JAR file not found. Build failed."
    exit 1
fi

echo "✅ JAR build successful!"

# Build Docker image
echo "🐳 Building Docker image with pre-built JAR..."
docker build -f Dockerfile.k8s -t ${FULL_IMAGE_NAME} .

echo "✅ Docker image built successfully!"
echo "🏷️  Image: ${FULL_IMAGE_NAME}"

# Load image into Minikube if it's running
echo "📦 Loading image into Minikube..."
if minikube status >/dev/null 2>&1; then
    minikube image load ${FULL_IMAGE_NAME}
    echo "✅ Image loaded into Minikube successfully!"
else
    echo "⚠️  Minikube not running, skipping image load"
fi

# Verify the image
echo "📋 Image details:"
docker images ${IMAGE_NAME}:${IMAGE_TAG}

echo ""
echo "Next steps:"
echo "1. Create Kubernetes secret with your Azure credentials"
echo "2. Deploy to Kubernetes: ./scripts/deploy-k8s.sh"
echo "3. Or run locally: docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
