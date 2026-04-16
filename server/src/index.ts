import http from 'http';
import fs from 'fs';
import { app } from './app';
import { config } from './config';
import { prisma } from './prisma';
import { redis } from './redis';
import { initSocket } from './ws/socket';

async function main() {
  // Ensure uploads directory exists
  if (!fs.existsSync(config.uploads.dir)) {
    fs.mkdirSync(config.uploads.dir, { recursive: true });
  }

  // Connect Redis (non-fatal if unavailable)
  try {
    await redis.connect();
  } catch (err: any) {
    console.warn('[Redis] Could not connect:', err.message, '— caching disabled');
  }

  // Test DB connection
  await prisma.$connect();
  console.log('[DB] Connected to PostgreSQL');

  // Create HTTP server + Socket.IO
  const httpServer = http.createServer(app);
  initSocket(httpServer);

  httpServer.listen(config.port, '0.0.0.0', () => {
    console.log(`[Server] Running on port ${config.port} (${config.nodeEnv})`);
    console.log(`[Server] API: http://0.0.0.0:${config.port}/api`);
    console.log(`[Server] Health: http://0.0.0.0:${config.port}/health`);
  });

  // Graceful shutdown
  const shutdown = async () => {
    console.log('[Server] Shutting down...');
    httpServer.close();
    await prisma.$disconnect();
    await redis.quit();
    process.exit(0);
  };
  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

main().catch((err) => {
  console.error('[Server] Fatal error:', err);
  process.exit(1);
});
