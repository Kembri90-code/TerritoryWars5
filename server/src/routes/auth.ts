import { Router, Request, Response } from 'express';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import crypto from 'crypto';
import { z } from 'zod';
import { prisma } from '../prisma';
import { config } from '../config';
import { requireAuth } from '../middleware/auth';

const router = Router();

const RegisterSchema = z.object({
  username: z.string().min(3).max(30).regex(/^[\w\sА-Яа-яёЁ]+$/u),
  email: z.string().email(),
  password: z.string().min(6).max(100),
  city_id: z.number().int().positive(),
});

const LoginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

function generateAccessToken(userId: string, email: string): string {
  return jwt.sign({ userId, email }, config.jwt.accessSecret, {
    expiresIn: config.jwt.accessExpiresIn as jwt.SignOptions['expiresIn'],
  });
}

function generateRefreshToken(): string {
  return crypto.randomBytes(64).toString('hex');
}

async function hashToken(token: string): Promise<string> {
  return crypto.createHash('sha256').update(token).digest('hex');
}

async function buildUserResponse(userId: string) {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    include: { city: true },
  });
  if (!user) return null;
  const clan = await getClanMetaSafe(user.clanId);
  return formatUser(user, clan);
}

async function getClanMetaSafe(clanId: string | null) {
  if (!clanId) return null;
  try {
    return await prisma.clan.findUnique({
      where: { id: clanId },
      select: { name: true, tag: true },
    });
  } catch {
    // Keep auth flow working even if clan tables/migrations are unavailable.
    return null;
  }
}

function formatUser(user: any, clan: { name: string; tag: string } | null) {
  return {
    id: user.id,
    email: user.email,
    username: user.username,
    avatar_url: user.avatarUrl,
    color: user.color,
    city_id: user.cityId,
    city_name: user.city?.name ?? null,
    clan_id: user.clanId ?? null,
    clan_name: clan?.name ?? null,
    clan_tag: clan?.tag ?? null,
    total_area_m2: user.totalAreaM2,
    territories_count: user.territoriesCount,
    captures_count: user.capturesCount,
    takeovers_count: user.takeoversCount,
    distance_walked_m: user.distanceWalkedM,
    created_at: user.createdAt.toISOString(),
  };
}

// POST /auth/register
router.post('/register', async (req: Request, res: Response) => {
  const parsed = RegisterSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation error', details: parsed.error.flatten() });
    return;
  }
  const { username, email, password, city_id } = parsed.data;

  try {
    const [existingEmail, existingUsername, city] = await Promise.all([
      prisma.user.findUnique({ where: { email } }),
      prisma.user.findUnique({ where: { username } }),
      prisma.city.findUnique({ where: { id: city_id } }),
    ]);

    if (existingEmail) { res.status(409).json({ error: 'Email already in use' }); return; }
    if (existingUsername) { res.status(409).json({ error: 'Username already taken' }); return; }
    if (!city) { res.status(400).json({ error: 'Invalid city_id' }); return; }

    const passwordHash = await bcrypt.hash(password, 12);
    const user = await prisma.user.create({
      data: { email, username, passwordHash, cityId: city_id },
      include: { city: true },
    });

    const accessToken = generateAccessToken(user.id, user.email);
    const refreshToken = generateRefreshToken();
    const tokenHash = await hashToken(refreshToken);
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);

    await prisma.refreshToken.create({
      data: { userId: user.id, tokenHash, expiresAt },
    });

    const userResponse = await buildUserResponse(user.id);
    if (!userResponse) {
      res.status(500).json({ error: 'Internal server error' });
      return;
    }

    res.status(201).json({
      access_token: accessToken,
      refresh_token: refreshToken,
      user: userResponse,
    });
  } catch (err: any) {
    console.error('[Auth] Register error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /auth/login
router.post('/login', async (req: Request, res: Response) => {
  const parsed = LoginSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation error' });
    return;
  }
  const { email, password } = parsed.data;

  try {
    const user = await prisma.user.findUnique({
      where: { email },
      include: { city: true },
    });

    if (!user) { res.status(401).json({ error: 'Invalid credentials' }); return; }

    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) { res.status(401).json({ error: 'Invalid credentials' }); return; }

    const accessToken = generateAccessToken(user.id, user.email);
    const refreshToken = generateRefreshToken();
    const tokenHash = await hashToken(refreshToken);
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);

    await prisma.refreshToken.create({
      data: { userId: user.id, tokenHash, expiresAt },
    });

    const userResponse = await buildUserResponse(user.id);
    if (!userResponse) {
      res.status(500).json({ error: 'Internal server error' });
      return;
    }

    res.json({
      access_token: accessToken,
      refresh_token: refreshToken,
      user: userResponse,
    });
  } catch (err) {
    console.error('[Auth] Login error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /auth/refresh
router.post('/refresh', async (req: Request, res: Response) => {
  const { refresh_token } = req.body;
  if (!refresh_token) { res.status(400).json({ error: 'Missing refresh_token' }); return; }

  try {
    const tokenHash = await hashToken(refresh_token);
    const stored = await prisma.refreshToken.findFirst({
      where: { tokenHash, expiresAt: { gt: new Date() } },
      include: { user: true },
    });

    if (!stored) { res.status(401).json({ error: 'Invalid or expired refresh token' }); return; }

    const accessToken = generateAccessToken(stored.user.id, stored.user.email);
    res.json({ access_token: accessToken });
  } catch (err) {
    console.error('[Auth] Refresh error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /auth/logout
router.post('/logout', requireAuth, async (req: Request, res: Response) => {
  const { refresh_token } = req.body;
  if (refresh_token) {
    const tokenHash = await hashToken(refresh_token);
    await prisma.refreshToken.deleteMany({ where: { tokenHash } });
  }
  res.status(204).send();
});

// GET /auth/check-username?username=...
router.get('/check-username', async (req: Request, res: Response) => {
  const username = req.query.username as string;
  if (!username) { res.status(400).json({ error: 'Missing username' }); return; }

  const existing = await prisma.user.findUnique({ where: { username } });
  res.json({ available: !existing });
});

// GET /auth/check-email?email=...
router.get('/check-email', async (req: Request, res: Response) => {
  const email = req.query.email as string;
  if (!email) { res.status(400).json({ error: 'Missing email' }); return; }

  const existing = await prisma.user.findUnique({ where: { email } });
  res.json({ available: !existing });
});

export { router as authRouter, formatUser };
