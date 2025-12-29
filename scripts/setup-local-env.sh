#!/bin/bash
set -e

echo "=== Setting up local development environment ==="

echo ""
echo "Step 1: Starting podman machine (if not already running)..."
podman machine start || echo "Podman machine already running"

echo ""
echo "Step 2: Setting up local registry..."
# Check if registry is already running
if ! podman ps | grep -q registry; then
  echo "Starting local registry on port 5001..."
  podman run -d -p 5001:5000 --name registry docker.io/library/registry:2
else
  echo "Registry already running"
fi

echo ""
echo "Step 3: Verifying kubectl connection..."
kubectl cluster-info || echo "Warning: kubectl not connected to a cluster"

echo ""
echo "Step 4: Creating namespace (if needed)..."
kubectl create namespace mergesort || echo "Namespace already exists or not needed"

echo ""
echo "=== Setup complete ==="
echo ""
echo "To build and deploy locally, run:"
echo "  ./scripts/local-deploy.sh"
echo ""
echo "To test with act (GitHub Actions locally), run:"
echo "  sudo act -P ubuntu-latest=ghcr.io/catthehacker/ubuntu:act-latest \\"
echo "       --container-architecture linux/amd64 --var-file .env"
