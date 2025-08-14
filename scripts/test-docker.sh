#!/bin/bash
# Test Container-Entra-auth with Docker Compose (without Kubernetes)

set -e

echo "🐳 Testing Container-Entra-auth with Docker Compose"
echo "=================================================="

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

# Prompt for Azure Client Secret
echo ""
echo "🔐 Please provide your Azure Client Secret:"
read -s -p "Azure Client Secret: " AZURE_CLIENT_SECRET
echo ""

if [[ -z "$AZURE_CLIENT_SECRET" ]]; then
    echo "❌ Azure Client Secret is required"
    exit 1
fi

# Create temporary docker-compose file with the secret
cat > docker-compose.temp.yml << EOF
version: '3.8'

services:
  container-entra-auth:
    image: container-entra-auth:latest
    container_name: container-entra-auth-test
    ports:
      - "8080:8080"
    environment:
      # Spring Configuration
      - SPRING_PROFILES_ACTIVE=k8s
      - APP_NAME=container-entra-auth
      - SERVER_PORT=8080
      - MANAGEMENT_PORT=8080
      
      # Azure Entra ID Configuration
      - AZURE_CLIENT_ID=9f6cf3b0-c7fe-4220-86fd-a58ac086a692
      - AZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET}
      - AZURE_TENANT_ID=4f402a0a-52d8-435d-9834-23cc12ede3ff
      - AZURE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/azure
      
      # API Configuration
      - API_BASE_PATH=/api/v1
      - CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
      - CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
      - CORS_ALLOWED_HEADERS=*
      
      # Logging Configuration
      - LOG_LEVEL_ROOT=INFO
      - LOG_LEVEL_SECURITY=INFO
      - LOG_LEVEL_OAUTH2=INFO
      - LOG_LEVEL_APP=INFO
      
    restart: unless-stopped
    
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

networks:
  default:
    name: container-entra-auth-network
EOF

echo "🚀 Starting Container-Entra-auth..."
docker-compose -f docker-compose.temp.yml up -d

echo "⏳ Waiting for application to start..."
sleep 15

echo "🏥 Checking health status..."
if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "✅ Application is healthy!"
else
    echo "⚠️  Health check failed, but application might still be starting..."
fi

echo ""
echo "📊 Application Status:"
docker-compose -f docker-compose.temp.yml ps

echo ""
echo "📋 Application URLs:"
echo "  - Application: http://localhost:8080"
echo "  - Health Check: http://localhost:8080/actuator/health"
echo "  - API Base: http://localhost:8080/api/v1"
echo "  - OAuth Login: http://localhost:8080/api/v1/auth/login"

echo ""
echo "📋 Useful Commands:"
echo "  - View logs: docker-compose -f docker-compose.temp.yml logs -f"
echo "  - Stop: docker-compose -f docker-compose.temp.yml down"
echo "  - Restart: docker-compose -f docker-compose.temp.yml restart"

# Clean up temporary file
rm -f docker-compose.temp.yml

echo ""
echo "✅ Container-Entra-auth is running with Docker Compose!"
