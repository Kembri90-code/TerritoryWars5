#!/bin/bash
set -e

echo "=== TerritoryWars Backend Deployment ==="
SERVER="root@93.183.74.141"
REMOTE_DIR="/opt/territorywars"

echo "[1/6] Updating server packages..."
ssh $SERVER "apt update -q && apt upgrade -y -q && apt install -y -q curl git"

echo "[2/6] Installing Docker..."
ssh $SERVER "
  if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
  else
    echo 'Docker already installed'
  fi
"

echo "[3/6] Installing Docker Compose plugin..."
ssh $SERVER "
  if ! docker compose version &> /dev/null; then
    apt install -y docker-compose-plugin
  else
    echo 'Docker Compose already installed'
  fi
"

echo "[4/6] Creating remote directory..."
ssh $SERVER "mkdir -p $REMOTE_DIR/prisma/migrations"

echo "[5/6] Copying files to server..."
scp package.json tsconfig.json docker-compose.yml Dockerfile nginx.conf .env $SERVER:$REMOTE_DIR/
scp -r src $SERVER:$REMOTE_DIR/
scp prisma/schema.prisma $SERVER:$REMOTE_DIR/prisma/
scp prisma/seed.ts $SERVER:$REMOTE_DIR/prisma/
scp prisma/migrations/add_postgis_polygon.sql $SERVER:$REMOTE_DIR/prisma/migrations/
scp .dockerignore $SERVER:$REMOTE_DIR/

echo "[6/6] Building and starting containers..."
ssh $SERVER "
  cd $REMOTE_DIR
  docker compose down --remove-orphans 2>/dev/null || true
  docker compose build --no-cache
  docker compose up -d

  echo 'Waiting for DB to be ready...'
  sleep 10

  echo 'Running Prisma migrations...'
  docker compose exec api npx prisma db push --force-reset 2>/dev/null || \
  docker compose exec api npx prisma db push

  echo 'Applying PostGIS column migration...'
  docker compose exec db psql -U territorywars -d territorywars -f /dev/stdin < prisma/migrations/add_postgis_polygon.sql || true

  echo 'Running seed...'
  docker compose exec api node -e \"
    const { PrismaClient } = require('@prisma/client');
    const prisma = new PrismaClient();
  \" 2>/dev/null || true

  docker compose logs api --tail=20
"

echo ""
echo "=== Deployment complete! ==="
echo "API:    http://93.183.74.141:3000/api"
echo "Health: http://93.183.74.141/health"
echo "WS:     ws://93.183.74.141/socket.io"
