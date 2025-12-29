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
