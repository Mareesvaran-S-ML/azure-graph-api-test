#!/bin/bash
# Create Kubernetes secret with Azure Entra ID credentials from .env file

set -e

echo "🔐 Creating Azure Entra ID Secret for Kubernetes"
echo "==============================================="

# Check if .env file exists
if [[ ! -f ".env" ]]; then
    echo "❌ .env file not found in current directory"
    echo "Please create a .env file with the following variables:"
    echo "AZURE_CLIENT_ID=your_client_id"
    echo "AZURE_CLIENT_SECRET=your_client_secret"
    echo "AZURE_TENANT_ID=your_tenant_id"
    exit 1
fi

echo "📄 Loading Azure credentials from .env file..."

# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

# Set defaults for optional variables (used by container-login-service)
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-production}"
LOGGING_LEVEL_ROOT="${LOGGING_LEVEL_ROOT:-INFO}"
LOGGING_LEVEL_SECURITY="${LOGGING_LEVEL_SECURITY:-DEBUG}"
SERVER_PORT="${SERVER_PORT:-8080}"

# Validate required inputs
if [[ -z "$AZURE_CLIENT_ID" || -z "$AZURE_CLIENT_SECRET" || -z "$AZURE_TENANT_ID" ]]; then
    echo "❌ Missing required environment variables in .env file"
    echo "Required variables: AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID"
    exit 1
fi

echo "✅ Loaded configuration from .env file"
echo "   Client ID: ${AZURE_CLIENT_ID:0:8}..."
echo "   Tenant ID: ${AZURE_TENANT_ID:0:8}..."
echo "   Spring Profile: $SPRING_PROFILES_ACTIVE"
echo "   Server Port: $SERVER_PORT"
echo "   Log Level: $LOGGING_LEVEL_ROOT"

echo ""
echo "🔐 Creating shared Kubernetes secret for both services..."

# Create the secret with all configuration (shared by both services)
kubectl create secret generic container-entra-auth-secret \
    --namespace=dev \
    --from-literal=AZURE_CLIENT_ID="$AZURE_CLIENT_ID" \
    --from-literal=AZURE_CLIENT_SECRET="$AZURE_CLIENT_SECRET" \
    --from-literal=AZURE_TENANT_ID="$AZURE_TENANT_ID" \
    --from-literal=SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
    --from-literal=LOGGING_LEVEL_ROOT="$LOGGING_LEVEL_ROOT" \
    --from-literal=LOGGING_LEVEL_SECURITY="$LOGGING_LEVEL_SECURITY" \
    --from-literal=SERVER_PORT="$SERVER_PORT" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "✅ Shared secret created successfully from .env file!"
echo "   This secret is used by both azure-graph-api-test and container-login-service"
echo ""
echo "🔍 To verify the secret:"
echo "kubectl get secret container-entra-auth-secret -n dev"
echo ""
echo "⚠️  Security Note: The secret is now stored in Kubernetes."
echo "   Make sure your cluster is properly secured and .env file is not committed to git."
