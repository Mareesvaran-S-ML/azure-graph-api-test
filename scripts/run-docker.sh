#!/bin/bash
# Run Container-Entra-auth with Docker (Host Network for Azure AD access)

set -e

echo "🐳 Running Container-Entra-auth with Docker"
echo "=========================================="

# Get the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to project root directory
cd "${PROJECT_ROOT}"

echo "📁 Project root: ${PROJECT_ROOT}"

# Check if Docker image exists
if ! docker images container-entra-auth:latest | grep -q container-entra-auth; then
    echo "❌ Docker image not found. Building first..."
    ./scripts/build-k8s.sh
fi

# Stop and remove existing container if running
if docker ps -a | grep -q container-entra-auth-prod; then
    echo "🛑 Stopping existing container..."
    docker rm -f container-entra-auth-prod
fi

# Prompt for Azure credentials
echo ""
echo "🔐 Azure Entra ID Configuration:"
echo "================================"

read -p "Azure Client ID: " AZURE_CLIENT_ID
read -s -p "Azure Client Secret: " AZURE_CLIENT_SECRET
echo ""
read -p "Azure Tenant ID: " AZURE_TENANT_ID
read -p "Application Port (default 8087): " APP_PORT

# Set default port if not provided
APP_PORT=${APP_PORT:-8087}

echo ""
echo "🚀 Starting Container-Entra-auth on port ${APP_PORT}..."

# Run container
docker run -d \
  --name container-entra-auth-prod \
  --network host \
  --restart unless-stopped \
  -e SPRING_PROFILES_ACTIVE=k8s \
  -e AZURE_CLIENT_ID="${AZURE_CLIENT_ID}" \
  -e AZURE_CLIENT_SECRET="${AZURE_CLIENT_SECRET}" \
  -e AZURE_TENANT_ID="${AZURE_TENANT_ID}" \
  -e AZURE_REDIRECT_URI="http://localhost:${APP_PORT}/login/oauth2/code/azure" \
  -e API_BASE_PATH=/api/v1 \
  -e SERVER_PORT="${APP_PORT}" \
  -e MANAGEMENT_PORT="${APP_PORT}" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:${APP_PORT}" \
  -e CORS_ALLOWED_METHODS="GET,POST,PUT,DELETE,OPTIONS" \
  -e CORS_ALLOWED_HEADERS="*" \
  -e LOG_LEVEL_ROOT=INFO \
  -e LOG_LEVEL_SECURITY=INFO \
  -e LOG_LEVEL_OAUTH2=INFO \
  -e LOG_LEVEL_APP=INFO \
  container-entra-auth:latest

echo "⏳ Waiting for application to start..."
sleep 15

# Check if container is running
if docker ps | grep -q container-entra-auth-prod; then
    echo "✅ Container is running!"
    
    # Test health endpoint
    if curl -f http://localhost:${APP_PORT}/health >/dev/null 2>&1; then
        echo "✅ Health check passed!"
    else
        echo "⚠️  Health check failed, but container is running"
    fi
    
    echo ""
    echo "📋 Application URLs:"
    echo "  - Health: http://localhost:${APP_PORT}/health"
    echo "  - API Base: http://localhost:${APP_PORT}/api/v1"
    echo "  - Auth Login: http://localhost:${APP_PORT}/api/auth/login"
    echo ""
    echo "📋 Management Commands:"
    echo "  - View logs: docker logs -f container-entra-auth-prod"
    echo "  - Stop: docker stop container-entra-auth-prod"
    echo "  - Remove: docker rm -f container-entra-auth-prod"
    echo ""
    echo "🧪 Test Authentication:"
    echo "curl -X POST http://localhost:${APP_PORT}/api/auth/login \\"
    echo "  -H 'Content-Type: application/json' \\"
    echo "  -d '{\"username\":\"your-email@domain.com\",\"password\":\"your-password\"}'"
    
else
    echo "❌ Container failed to start. Check logs:"
    docker logs container-entra-auth-prod
    exit 1
fi

echo ""
echo "✅ Container-Entra-auth is running with Docker!"
