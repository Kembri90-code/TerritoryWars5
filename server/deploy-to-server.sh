#!/bin/bash
# Run from your LOCAL machine (Windows WSL or Git Bash)
# Requires: SSH access to root@93.183.74.141
set -e

SERVER="root@93.183.74.141"
REMOTE_DIR="/opt/territorywars"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== [1/5] Preparing server ==="
ssh -o StrictHostKeyChecking=no $SERVER "
  apt update -qq && apt upgrade -y -qq
  apt install -y -qq curl git
  if ! command -v docker &>/dev/null; then
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker && systemctl start docker
  fi
  if ! docker compose version &>/dev/null; then
    apt install -y docker-compose-plugin
  fi
  mkdir -p $REMOTE_DIR/prisma/migrations $REMOTE_DIR/src
"

echo "=== [2/5] Uploading files ==="
rsync -avz --exclude='node_modules' --exclude='dist' --exclude='.git' \
  "$SCRIPT_DIR/" $SERVER:$REMOTE_DIR/

echo "=== [3/5] Building and starting ==="
ssh $SERVER "
  cd $REMOTE_DIR
  docker compose down --remove-orphans 2>/dev/null || true
  docker compose build --no-cache
  docker compose up -d
  echo 'Waiting 20s for DB...'
  sleep 20
"

echo "=== [4/5] Running migrations and seed ==="
ssh $SERVER "
  cd $REMOTE_DIR

  # Prisma push
  docker compose exec -T api npx prisma db push

  # PostGIS polygon column
  docker compose exec -T db psql -U territorywars -d territorywars < prisma/migrations/add_postgis_polygon.sql

  # Seed cities
  docker compose exec -T api node -e \"
    const { PrismaClient } = require('@prisma/client');
    const fs = require('fs');
    const cities = JSON.parse(fs.readFileSync('/app/prisma/seed-data.json','utf8'));
    const p = new PrismaClient();
    async function run() {
      for (const c of cities) {
        await p.city.upsert({ where: { id: c.id }, create: c, update: c });
      }
      console.log('Seeded', cities.length, 'cities');
      await p.\\\$disconnect();
    }
    run().catch(e => { console.error(e); process.exit(1); });
  \"
"

echo "=== [5/5] Health check ==="
sleep 5
curl -sf http://93.183.74.141/health && echo "  OK" || echo "  FAILED - check logs"
echo ""
ssh $SERVER "docker compose -f $REMOTE_DIR/docker-compose.yml ps"

echo ""
echo "=============================="
echo " Deployment complete!"
echo " API:    http://93.183.74.141/api"
echo " Health: http://93.183.74.141/health"
echo " WS:     ws://93.183.74.141/socket.io"
echo "=============================="
