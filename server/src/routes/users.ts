import { Router, Request, Response } from 'express';
import { z } from 'zod';
import path from 'path';
import fs from 'fs';
import { prisma } from '../prisma';
import { requireAuth } from '../middleware/auth';
import { uploadAvatar } from '../middleware/upload';
import { config } from '../config';

const router = Router();

function formatUser(user: any) {
  return {
    id: user.id,
    email: user.email,
    username: user.username,
    avatar_url: user.avatarUrl,
    color: user.color,
    city_id: user.cityId,
    city_name: user.city?.name ?? null,
    clan_id: user.clanId ?? null,
    clan_name: user.clanMembership?.clan?.name ?? null,
    clan_tag: user.clanMembership?.clan?.tag ?? null,
    total_area_m2: user.totalAreaM2,
    territories_count: user.territoriesCount,
    captures_count: user.capturesCount,
    takeovers_count: user.takeoversCount,
    distance_walked_m: user.distanceWalkedM,
    created_at: user.createdAt.toISOString(),
  };
}

const includeUserRelations = {
  city: true,
  clanMembership: { include: { clan: true } },
};

const UpdateProfileSchema = z.object({
  username: z.string().min(3).max(30).regex(/^[\w\sА-Яа-яёЁ]+$/u).optional(),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/).optional(),
  city_id: z.number().int().positive().optional(),
});

// GET /users/me
router.get('/me', requireAuth, async (req: Request, res: Response) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user!.userId },
      include: includeUserRelations,
    });
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }
    res.json(formatUser(user));
  } catch (err) {
    console.error('[Users] getMe error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /users/me
router.put('/me', requireAuth, async (req: Request, res: Response) => {
  const parsed = UpdateProfileSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation error', details: parsed.error.flatten() });
    return;
  }
  const { username, color, city_id } = parsed.data;

  try {
    if (username) {
      const existing = await prisma.user.findFirst({
        where: { username, NOT: { id: req.user!.userId } },
      });
      if (existing) { res.status(409).json({ error: 'Username already taken' }); return; }
    }
    if (city_id) {
      const city = await prisma.city.findUnique({ where: { id: city_id } });
      if (!city) { res.status(400).json({ error: 'Invalid city_id' }); return; }
    }

    const updated = await prisma.user.update({
      where: { id: req.user!.userId },
      data: {
        ...(username && { username }),
        ...(color && { color }),
        ...(city_id && { cityId: city_id }),
      },
      include: includeUserRelations,
    });
    res.json(formatUser(updated));
  } catch (err) {
    console.error('[Users] updateProfile error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /users/me/avatar
router.post('/me/avatar', requireAuth, (req: Request, res: Response) => {
  uploadAvatar(req, res, async (err) => {
    if (err) { res.status(400).json({ error: err.message }); return; }
    if (!req.file) { res.status(400).json({ error: 'No file uploaded' }); return; }

    try {
      const user = await prisma.user.findUnique({ where: { id: req.user!.userId } });
      // Delete old avatar if it exists and is not a URL
      if (user?.avatarUrl && !user.avatarUrl.startsWith('http')) {
        const oldPath = path.join(config.uploads.dir, path.basename(user.avatarUrl));
        if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
      }

      const avatarUrl = `/uploads/${req.file.filename}`;
      const updated = await prisma.user.update({
        where: { id: req.user!.userId },
        data: { avatarUrl },
        include: includeUserRelations,
      });
      res.json(formatUser(updated));
    } catch (dbErr) {
      console.error('[Users] uploadAvatar error:', dbErr);
      res.status(500).json({ error: 'Internal server error' });
    }
  });
});

// GET /users/:id
router.get('/:id', requireAuth, async (req: Request, res: Response) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.params.id },
      include: includeUserRelations,
    });
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }
    res.json(formatUser(user));
  } catch (err) {
    console.error('[Users] getUserById error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export { router as usersRouter };
