#!/bin/bash
set -e

echo "=== Building Docker image with Podman ==="
podman build -t mergesort-server:latest .

echo ""
echo "=== Tagging image for local registry ==="
podman tag mergesort-server:latest localhost:5000/mergesort-server:latest

echo ""
echo "=== Pushing to local registry ==="
podman push localhost:5000/mergesort-server:latest

echo ""
echo "=== Applying Kubernetes manifests ==="
kubectl apply -f k8s/

echo ""
echo "=== Waiting for rollout to complete ==="
kubectl rollout status deployment/mergesort-server --timeout=300s

echo ""
echo "=== Deployment status ==="
kubectl get pods -l app=mergesort-server
kubectl get svc mergesort-server

echo ""
echo "=== Service endpoint ==="
NODE_PORT=$(kubectl get svc mergesort-server -o jsonpath='{.spec.ports[0].nodePort}')
echo "Server accessible at: localhost:$NODE_PORT"
