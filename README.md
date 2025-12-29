# Parallel Merge Sort Client-Server

Client sends an integer array and requested thread count; server performs a guarded parallel merge sort and returns the sorted array.

Key points:

- Protocol: `ObjectOutputStream` to send array + thread count; `ObjectInputStream` to receive sorted array.
- Parallelism bounded by requested threads and CPU cores on the server.
- Benchmarking is available from the client via `benchmark <size> <maxThreads> <runs>`.

## How parallel merge works

- Partition: Split the input into N chunks, where N ≤ requested threads and bounded by array size.
- Parallel sort: Sort each chunk independently using a fixed-size thread pool.
- Iterative merge: Merge sorted chunks in rounds; each round merges pairs of chunks (often in parallel), halving the number of chunks until one remains.
- Guarding: Cap threads to avoid oversubscription and size chunks to balance sort/merge work.

## Deployment

### Local Development with Podman & Kubernetes

1. **Setup local environment:**
   ```bash
   chmod +x scripts/setup-local-env.sh scripts/local-deploy.sh
   ./scripts/setup-local-env.sh
   ```

2. **Build and deploy:**
   ```bash
   ./scripts/local-deploy.sh
   ```

3. **Access the service:**
   ```bash
   NODE_PORT=$(kubectl get svc mergesort-server -o jsonpath='{.spec.ports[0].nodePort}')
   echo "Server running at localhost:$NODE_PORT"
   ```

### Testing GitHub Actions Locally with act

1. **Copy environment variables:**
   ```bash
   cp .env.example .env
   # Edit .env if needed
   ```

2. **Run act:**
   ```bash
   sudo act -P ubuntu-latest=ghcr.io/catthehacker/ubuntu:act-latest \
       --container-architecture linux/amd64 --var-file .env
   ```

### Docker Build

```bash
# Build with Docker/Podman
podman build -t mergesort-server:latest .

# Run standalone
podman run -p 12345:12345 mergesort-server:latest
```

### Kubernetes Deployment

```bash
# Apply manifests
kubectl apply -f k8s/

# Check status
kubectl get pods -l app=mergesort-server
kubectl get svc mergesort-server

# Scale deployment
kubectl scale deployment/mergesort-server --replicas=3

# View logs
kubectl logs -l app=mergesort-server --tail=50 -f
```

## CI/CD Pipeline

The GitHub Actions workflow includes:
- Maven build and test
- SpotBugs static analysis
- Docker image build
- Trivy security scanning
- Kubernetes rolling deployment

## GitHub Actions Setup (Deploy to Local Kubernetes from GitHub Runners)

### Prerequisites

1. **Local Kubernetes cluster** (minikube, kind, k3s, or Docker Desktop)
2. **Local container registry** running on port 5000
3. **kubectl** configured to access your cluster
4. **Tunnel solution** (ngrok, Cloudflare Tunnel, or port forwarding)

### Step 1: Setup Local Container Registry

```bash
# Start a local registry
podman run -d -p 5000:5000 --name registry registry:2
```

### Step 2: Expose Local Registry via Tunnel

You need to expose your local registry to the internet so GitHub Actions can push images to it.

**Option A: Using ngrok (easiest)**
```bash
# Install ngrok: https://ngrok.com/download
# Sign up for free account and get auth token

# Expose registry
ngrok http 5000

# Note the URL: https://xxxx-xx-xx-xx-xx.ngrok-free.app
```

**Option B: Using Cloudflare Tunnel**
```bash
# Install: brew install cloudflare/cloudflare/cloudflared

# Login and create tunnel
cloudflared tunnel login
cloudflared tunnel create registry-tunnel
cloudflared tunnel route dns registry-tunnel registry.yourdomain.com

# Run tunnel
cloudflared tunnel --url http://localhost:5000 run registry-tunnel
```

**Option C: SSH Reverse Tunnel (if you have a public server)**
```bash
# From your Mac, create reverse tunnel to public server
ssh -R 5000:localhost:5000 user@your-public-server.com
```

### Step 3: Expose Kubernetes API via Tunnel

**Option A: Using ngrok**
```bash
# Get your K8s API server port
kubectl cluster-info | grep "control plane"

# For most local clusters, expose port 6443 or the appropriate port
ngrok tcp 6443  # or whatever port your API server uses

# Note the TCP address: tcp://x.tcp.ngrok.io:xxxxx
```

**Option B: Configure API Server for Remote Access**

For minikube:
```bash
# Start minikube with host access
minikube start --apiserver-ips=127.0.0.1

# Get the API server URL
kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'
```

### Step 4: Setup GitHub Secrets

Go to your GitHub repo → Settings → Secrets and variables → Actions → New repository secret

Add these secrets:

1. **LOCAL_REGISTRY_URL**: Your tunnel URL without protocol
   - Example: `xxxx.ngrok-free.app:5000` (ngrok)
   - Example: `registry.yourdomain.com` (Cloudflare)

2. **KUBE_CONFIG_BASE64**: Your kubeconfig encoded in base64
   ```bash
   # Get your kubeconfig
   cat ~/.kube/config | base64 | pbcopy
   
   # Or for specific context:
   kubectl config view --minify --flatten | base64 | pbcopy
   ```
   
   **Important**: Update the server URL in your kubeconfig to use the tunnel endpoint:
   ```bash
   # First, export your config
   kubectl config view --minify --flatten > /tmp/kubeconfig-for-gha.yaml
   
   # Edit /tmp/kubeconfig-for-gha.yaml and change the server URL to your tunnel
   # Example: server: https://x.tcp.ngrok.io:xxxxx
   
   # Then encode it
   cat /tmp/kubeconfig-for-gha.yaml | base64 | pbcopy
   ```

3. **REGISTRY_AUTH** (optional): If you secured your registry with basic auth
   ```bash
   echo -n "username:password" | base64
   ```

### Step 5: Update Kubernetes Deployment

Since GitHub will push to a different registry URL, update your deployment:

```bash
# Edit k8s/deployment.yaml to use the secret registry URL
# Or use kubectl to patch it after deployment
```

### Step 6: Keep Tunnels Running

For production use, keep your tunnels running as services:

**ngrok as a service** (macOS):
```bash
# Create ~/Library/LaunchAgents/com.ngrok.registry.plist
cat > ~/Library/LaunchAgents/com.ngrok.registry.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.ngrok.registry</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/local/bin/ngrok</string>
        <string>http</string>
        <string>5000</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
</dict>
</plist>
EOF

# Load the service
launchctl load ~/Library/LaunchAgents/com.ngrok.registry.plist
```

### Step 7: Test the Pipeline

```bash
git add .
git commit -m "Configure GHA for remote deployment to local K8s"
git push origin main
```

### Security Considerations

⚠️ **Important Security Notes:**

1. **Tunnels expose your services to the internet** - consider using authentication
2. **Kubeconfig contains sensitive credentials** - use GitHub secrets (encrypted)
3. **Registry authentication** - add basic auth to your registry:
   ```bash
   # Create htpasswd file
   docker run --entrypoint htpasswd httpd:2 -Bbn username password > auth/htpasswd
   
   # Run registry with auth
   podman run -d -p 5000:5000 --name registry \
     -v $(pwd)/auth:/auth \
     -e "REGISTRY_AUTH=htpasswd" \
     -e "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm" \
     -e "REGISTRY_AUTH_HTPASSWD_PATH=/auth/htpasswd" \
     registry:2
   ```

### Alternative: Webhook-based Deployment

If tunneling the entire K8s API is too complex, consider a simpler webhook approach:

1. Run a simple webhook server on your Mac that listens for deployment requests
2. GitHub Actions calls your webhook with the new image tag
3. Webhook server pulls image and updates K8s deployment locally

Let me know if you'd like me to set up this alternative approach!

## Benchmark

Source: `client/src/client/MergeSortClient.java` (`runBenchmark`)

Environment:

- size = 10,000,000
- maxThreads = 20
- runs = 5

Results:

| Threads | Avg (ms) |
|--------:|---------:|
| 1       | 1820.801 |
| 2       | 1562.826 |
| 3       | 1490.661 |
| 4       | 1476.961 |
| 5       | 1445.143 |
| 6       | 1425.110 |
| 7       | 1428.679 |
| 8       | 1432.627 |
| 9       | 1416.640 |
| 10      | 1446.206 |
| 11      | 1440.372 |
| 12      | 1430.068 |
| 13      | 1441.786 |
| 14      | 1446.090 |
| 15      | 1443.182 |

> Observations:
>
> - Clear gains from 1 to ~6 threads.
> - Diminishing returns beyond ~6–9 threads due to merge synchronization, memory bandwidth, and network overhead.
