#!/usr/bin/env bash
set -euo pipefail

SERVER="93.183.74.141"
SSH_KEY="$HOME/Downloads/runterritory-api.pem"
REMOTE_DIR="/opt/territorywars"
ADMIN_DIST="admin/dist"

echo "=== Building admin panel ==="
cd admin
npm install
npm run build
cd ..

echo "=== Uploading admin dist to server ==="
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no root@"$SERVER" "mkdir -p $REMOTE_DIR/admin/dist"
scp -i "$SSH_KEY" -r admin/dist/. root@"$SERVER":"$REMOTE_DIR/admin/dist/"

echo "=== Uploading updated server files ==="
scp -i "$SSH_KEY" server/src/routes/admin.ts root@"$SERVER":/tmp/admin_route.ts
scp -i "$SSH_KEY" server/src/middleware/adminAuth.ts root@"$SERVER":/tmp/adminAuth.ts
scp -i "$SSH_KEY" server/src/app.ts root@"$SERVER":/tmp/app.ts
scp -i "$SSH_KEY" server/nginx.conf root@"$SERVER":"$REMOTE_DIR/server/nginx.conf"
scp -i "$SSH_KEY" server/docker-compose.yml root@"$SERVER":"$REMOTE_DIR/server/docker-compose.yml"
scp -i "$SSH_KEY" server/prisma/schema.prisma root@"$SERVER":"$REMOTE_DIR/server/prisma/schema.prisma"

echo "=== Copying source files into server src ==="
ssh -i "$SSH_KEY" root@"$SERVER" "
  cp /tmp/admin_route.ts $REMOTE_DIR/server/src/routes/admin.ts
  cp /tmp/adminAuth.ts $REMOTE_DIR/server/src/middleware/adminAuth.ts
  cp /tmp/app.ts $REMOTE_DIR/server/src/app.ts
"

echo "=== Rebuilding and restarting containers ==="
ssh -i "$SSH_KEY" root@"$SERVER" "
  cd $REMOTE_DIR/server
  docker compose build api
  docker compose up -d
  docker compose exec -T api npx prisma db push --skip-generate
"

echo ""
echo "=== Done! ==="
echo "Admin panel: http://$SERVER/admin/"
echo "API:         http://$SERVER/api/"
