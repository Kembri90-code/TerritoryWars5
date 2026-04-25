import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config';
import { redis } from '../redis';

export interface JwtPayload {
  userId: string;
  email: string;
}

declare global {
  namespace Express {
    interface Request {
      user?: JwtPayload;
    }
  }
}

export function requireAuth(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Unauthorized' });
    return;
  }

  const token = authHeader.slice(7);
  try {
    const payload = jwt.verify(token, config.jwt.accessSecret) as JwtPayload;
    req.user = payload;
    // DAU tracking: add userId to Redis SET for today (fire-and-forget)
    const today = new Date().toISOString().slice(0, 10);
    redis.sadd(`dau:${today}`, payload.userId).then(() => redis.expire(`dau:${today}`, 30 * 86400)).catch(() => {});
    next();
  } catch {
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}
