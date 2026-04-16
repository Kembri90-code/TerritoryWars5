import { Server as HttpServer } from 'http';
import { Server as SocketIO, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { JwtPayload } from '../middleware/auth';

let io: SocketIO | null = null;

export function initSocket(httpServer: HttpServer): SocketIO {
  io = new SocketIO(httpServer, {
    cors: { origin: config.cors.origin, methods: ['GET', 'POST'] },
    pingTimeout: 30000,
    pingInterval: 10000,
  });

  // Auth middleware
  io.use((socket, next) => {
    const token = socket.handshake.auth?.token as string | undefined;
    if (!token) {
      return next(new Error('Authentication required'));
    }
    try {
      const payload = jwt.verify(token, config.jwt.accessSecret) as JwtPayload;
      (socket as any).user = payload;
      next();
    } catch {
      next(new Error('Invalid token'));
    }
  });

  io.on('connection', (socket: Socket) => {
    const user = (socket as any).user as JwtPayload;
    console.log(`[WS] User connected: ${user.userId}`);

    // Join personal room
    socket.join(`user:${user.userId}`);

    // Join bbox region room (client sends current map bbox)
    socket.on('subscribe_bbox', (bbox: string) => {
      // Leave old bbox rooms
      socket.rooms.forEach((room) => {
        if (room.startsWith('bbox:')) socket.leave(room);
      });
      // Encode bbox to room name (rough grid)
      const parts = bbox.split(',').map(Number);
      if (parts.length === 4 && parts.every((n) => !isNaN(n))) {
        const roomName = bboxToRoom(parts[0], parts[1], parts[2], parts[3]);
        socket.join(roomName);
      }
    });

    // Player location update (broadcast to nearby users)
    socket.on('location_update', (data: { lat: number; lng: number }) => {
      socket.rooms.forEach((room) => {
        if (room.startsWith('bbox:')) {
          socket.to(room).emit('player_moved', {
            userId: user.userId,
            lat: data.lat,
            lng: data.lng,
            timestamp: Date.now(),
          });
        }
      });
    });

    socket.on('disconnect', () => {
      console.log(`[WS] User disconnected: ${user.userId}`);
    });
  });

  return io;
}

// Broadcast a new/updated territory to all clients in overlapping rooms
export function emitTerritoryUpdate(territory: any): void {
  if (!io) return;
  io.emit('territory_updated', territory);
}

// Broadcast territory deletion
export function emitTerritoryDeleted(territoryId: string): void {
  if (!io) return;
  io.emit('territory_deleted', { id: territoryId });
}

function bboxToRoom(lat1: number, lng1: number, lat2: number, lng2: number): string {
  // Snap to ~1° grid for rooms
  const minLat = Math.floor(Math.min(lat1, lat2));
  const minLng = Math.floor(Math.min(lng1, lng2));
  const maxLat = Math.ceil(Math.max(lat1, lat2));
  const maxLng = Math.ceil(Math.max(lng1, lng2));
  return `bbox:${minLat}_${minLng}_${maxLat}_${maxLng}`;
}
