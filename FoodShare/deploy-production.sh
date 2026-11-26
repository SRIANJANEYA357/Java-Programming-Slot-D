#!/bin/bash
set -e

echo "=========================================================="
echo "FOODSHARE PRODUCTION CONTAINER DEPLOYMENT & VERIFICATION"
echo "=========================================================="

# 1. Build Production Docker Image
echo "[1/4] Building Docker image foodshare:1.0.0..."
docker build -t foodshare:1.0.0 .

# 2. Start Services via Docker Compose
echo "[2/4] Deploying containers via docker-compose.prod.yml..."
docker compose -f docker-compose.prod.yml up -d

# 3. Wait for Healthcheck
echo "[3/4] Awaiting container healthy status..."
for i in {1..30}; do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' foodshare-prod-app 2>/dev/null || echo "starting")
  if [ "$STATUS" == "\"healthy\"" ]; then
    echo "FoodShare container is HEALTHY!"
    break
  fi
  echo "Waiting for health check... ($i/30)"
  sleep 3
done

# 4. Run Live Verification
echo "[4/4] Running automated end-to-end verification..."
python3 verify_production_live.py http://localhost:8088

echo "=========================================================="
echo "DEPLOYMENT COMPLETE & VERIFIED SUCCESSFULLY!"
echo "=========================================================="
