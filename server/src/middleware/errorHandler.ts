import { Request, Response, NextFunction } from 'express';
import { redis } from '../redis';

export function errorHandler(err: Error, req: Request, res: Response, _next: NextFunction): void {
  console.error('[Error]', err.message, err.stack);
  const today = new Date().toISOString().slice(0, 10);
  redis.incr(`errors:${today}`).then(() => redis.expire(`errors:${today}`, 30 * 86400)).catch(() => {});
  res.status(500).json({ error: 'Internal server error' });
}
