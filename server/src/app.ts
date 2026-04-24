import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import path from 'path';
import { config } from './config';
import { authRouter } from './routes/auth';
import { usersRouter } from './routes/users';
import { territoriesRouter, getUserTerritoriesHandler } from './routes/territories';
import { clansRouter } from './routes/clans';
import { leaderboardRouter } from './routes/leaderboard';
import { citiesRouter } from './routes/cities';
import { adminRouter } from './routes/admin';
import { requireAuth } from './middleware/auth';
import { errorHandler } from './middleware/errorHandler';

const app = express();

// Trust Nginx reverse proxy (required for express-rate-limit behind proxy)
app.set('trust proxy', 1);

// Security
app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));
app.use(cors({ origin: config.cors.origin }));

// Rate limiting
const limiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 300, standardHeaders: true, legacyHeaders: false });
const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 30, standardHeaders: true, legacyHeaders: false });
app.use('/api/auth', authLimiter);
app.use(limiter);

// Body parsing
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true }));

// Static uploads
app.use('/uploads', express.static(config.uploads.dir, { maxAge: '7d' }));

// Health check
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', version: '1.0.0', timestamp: new Date().toISOString() });
});

// Routes
app.use('/api/auth', authRouter);
app.use('/api/users', usersRouter);
app.use('/api/territories', territoriesRouter);
app.use('/api/clans', clansRouter);
app.use('/api/leaderboard', leaderboardRouter);
app.use('/api/cities', citiesRouter);
app.use('/api/admin', adminRouter);

// GET /api/users/:id/territories
app.get('/api/users/:id/territories', requireAuth, getUserTerritoriesHandler);

// 404 handler
app.use((_req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// Error handler
app.use(errorHandler);

export { app };
