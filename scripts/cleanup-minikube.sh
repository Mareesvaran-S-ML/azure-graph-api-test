#!/bin/bash

echo "🧹 Complete Minikube Cleanup Script"
echo "=================================="
echo "⚠️  WARNING: This will delete ALL Minikube data and Docker images!"
echo ""

# Function to ask for confirmation
confirm() {
    read -p "Are you sure you want to proceed? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Cleanup cancelled"
        exit 1
    fi
}

# Ask for confirmation
confirm

echo "🛑 Stopping all processes..."

# Kill any running port-forwards
echo "🔌 Killing port-forward processes..."
pkill -f "kubectl port-forward" || echo "No port-forward processes found"

# Stop Minikube
echo "🛑 Stopping Minikube..."
minikube stop || echo "Minikube was not running"

# Delete Minikube cluster
echo "🗑️  Deleting Minikube cluster..."
minikube delete || echo "No Minikube cluster found"

# Clean up Minikube profiles
echo "🗑️  Cleaning up Minikube profiles..."
minikube delete --all || echo "No profiles to delete"

# Remove Minikube configuration
echo "🗑️  Removing Minikube configuration..."
rm -rf ~/.minikube || echo "No Minikube config found"

echo "✅ Minikube cleanup completed"

echo ""
echo "🐳 Docker Cleanup"
echo "================="

# Stop all running containers
echo "🛑 Stopping all Docker containers..."
docker stop $(docker ps -aq) 2>/dev/null || echo "No containers to stop"

# Remove all containers
echo "🗑️  Removing all Docker containers..."
docker rm $(docker ps -aq) 2>/dev/null || echo "No containers to remove"

# Remove project-specific images
echo "🗑️  Removing project Docker images..."
docker rmi container-login-service:latest 2>/dev/null || echo "container-login-service image not found"
docker rmi container-entra-auth:latest 2>/dev/null || echo "container-entra-auth image not found"

# Remove dangling images
echo "🗑️  Removing dangling Docker images..."
docker image prune -f || echo "No dangling images to remove"

# Remove all unused images (optional - uncomment if needed)
# echo "🗑️  Removing all unused Docker images..."
# docker image prune -a -f

# Remove all volumes
echo "🗑️  Removing Docker volumes..."
docker volume prune -f || echo "No volumes to remove"

# Remove all networks
echo "🗑️  Removing Docker networks..."
docker network prune -f || echo "No networks to remove"

# System cleanup
echo "🗑️  Docker system cleanup..."
docker system prune -f || echo "System cleanup completed"

echo "✅ Docker cleanup completed"

echo ""
echo "🧹 Build Artifacts Cleanup"
echo "=========================="

# Clean Maven artifacts
echo "🗑️  Cleaning Maven artifacts..."
./mvnw clean -q 2>/dev/null || echo "Maven clean skipped"
cd container-login-service && ./mvnw clean -q 2>/dev/null || echo "Container service Maven clean skipped"
cd ..

# Remove target directories
echo "🗑️  Removing target directories..."
find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || echo "No target directories found"

# Remove log files
echo "🗑️  Removing log files..."
find . -name "*.log" -type f -delete 2>/dev/null || echo "No log files found"

# Remove temporary files
echo "🗑️  Removing temporary files..."
find . -name "*.tmp" -type f -delete 2>/dev/null || echo "No temp files found"
find . -name ".DS_Store" -type f -delete 2>/dev/null || echo "No .DS_Store files found"

echo "✅ Build artifacts cleanup completed"

echo ""
echo "🎉 Complete Cleanup Finished!"
echo "============================="
echo "✅ Minikube cluster deleted"
echo "✅ Docker containers and images removed"
echo "✅ Build artifacts cleaned"
echo "✅ Temporary files removed"
echo ""
echo "💡 To start fresh, run: ./scripts/deploy-azure-services.sh"
