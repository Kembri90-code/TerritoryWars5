#!/bin/bash
# Run this script ON THE SERVER after copying files
# ssh root@93.183.74.141
# cd /opt/territorywars && bash setup-server.sh

set -e

echo "=== Server Setup ==="

# Update system
apt update -q && apt upgrade -y -q
apt install -y -q curl git wget

# Install Docker
if ! command -v docker &> /dev/null; then
  echo "Installing Docker..."
  curl -fsSL https://get.docker.com | sh
  systemctl enable docker
  systemctl start docker
fi

# Install Docker Compose plugin
if ! docker compose version &> /dev/null; then
  apt install -y docker-compose-plugin
fi

echo "Docker: $(docker --version)"
echo "Compose: $(docker compose version)"

# Build & start
echo "=== Building containers ==="
docker compose down --remove-orphans 2>/dev/null || true
docker compose build
docker compose up -d

echo "Waiting 15s for services to be ready..."
sleep 15

# Run Prisma push (create tables)
echo "=== Running Prisma DB Push ==="
docker compose exec -T api npx prisma db push

# Apply PostGIS column (must be after prisma creates the table)
echo "=== Applying PostGIS migration ==="
docker compose exec -T db psql -U territorywars -d territorywars < prisma/migrations/add_postgis_polygon.sql

# Seed cities
echo "=== Seeding cities ==="
docker compose exec -T api node -e "
const { PrismaClient } = require('@prisma/client');
const p = new PrismaClient();
const cities = $(cat prisma/seed-data.json);
async function run() {
  for (const c of cities) {
    await p.city.upsert({ where: { id: c.id }, create: c, update: c });
  }
  console.log('Cities seeded:', cities.length);
  await p.\$disconnect();
}
run().catch(e => { console.error(e); process.exit(1); });
"

# Check health
echo "=== Health Check ==="
sleep 3
curl -f http://localhost:3000/health && echo "" || echo "Health check failed!"

echo ""
echo "=== Setup Complete! ==="
docker compose ps
