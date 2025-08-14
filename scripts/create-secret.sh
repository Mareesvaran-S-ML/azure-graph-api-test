#!/bin/bash
# Create Kubernetes secret with Azure Entra ID credentials

set -e

echo "🔐 Creating Azure Entra ID Secret for Kubernetes"
echo "==============================================="

# Prompt for Azure credentials
echo "Please provide your Azure Entra ID credentials:"
echo ""

read -p "Azure Client ID: " AZURE_CLIENT_ID
read -s -p "Azure Client Secret: " AZURE_CLIENT_SECRET
echo ""
read -p "Azure Tenant ID: " AZURE_TENANT_ID

# Validate inputs
if [[ -z "$AZURE_CLIENT_ID" || -z "$AZURE_CLIENT_SECRET" || -z "$AZURE_TENANT_ID" ]]; then
    echo "❌ All fields are required"
    exit 1
fi

echo ""
echo "🔐 Creating Kubernetes secret..."

# Create the secret
kubectl create secret generic container-entra-auth-secret \
    --namespace=dev \
    --from-literal=AZURE_CLIENT_ID="$AZURE_CLIENT_ID" \
    --from-literal=AZURE_CLIENT_SECRET="$AZURE_CLIENT_SECRET" \
    --from-literal=AZURE_TENANT_ID="$AZURE_TENANT_ID" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "✅ Secret created successfully!"
echo ""
echo "🔍 To verify the secret:"
echo "kubectl get secret container-entra-auth-secret -n dev"
echo ""
echo "⚠️  Security Note: The secret is now stored in Kubernetes."
echo "   Make sure your cluster is properly secured."
