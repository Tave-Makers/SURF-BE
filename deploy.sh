#!/bin/bash
set -e

cd "$(dirname "$0")"

echo ">>> Pulling latest image..."
docker compose pull tave-surf

echo ">>> Restarting application..."
docker compose up -d

echo ">>> Cleaning up old images..."
docker image prune -f

echo ">>> Deploy complete!"
docker compose ps
