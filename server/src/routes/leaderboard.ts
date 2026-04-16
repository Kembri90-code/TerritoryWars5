import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';
import { requireAuth } from '../middleware/auth';
import { redis } from '../redis';

const router = Router();
const CACHE_TTL = 60; // seconds

// GET /leaderboard/players?sort=area&limit=100
router.get('/players', requireAuth, async (req: Request, res: Response) => {
  const sort = (req.query.sort as string) || 'area';
  const limit = Math.min(parseInt(req.query.limit as string) || 100, 200);

  const sortMap: Record<string, string> = {
    area: 'total_area_m2',
    captures: 'captures_count',
    distance: 'distance_walked_m',
  };
  const orderCol = sortMap[sort] ?? 'total_area_m2';
  const cacheKey = `leaderboard:players:${sort}:${limit}`;

  try {
    const cached = await redis.get(cacheKey);
    if (cached) {
      res.json(JSON.parse(cached));
      return;
    }

    const rows = await prisma.$queryRawUnsafe<any[]>(`
      SELECT
        ROW_NUMBER() OVER (ORDER BY u.${orderCol} DESC) AS rank,
        u.id AS user_id,
        u.username,
        u.avatar_url,
        u.color,
        ci.name AS city_name,
        cl.tag AS clan_tag,
        u.total_area_m2,
        u.territories_count,
        u.captures_count,
        u.distance_walked_m
      FROM users u
      LEFT JOIN cities ci ON ci.id = u.city_id
      LEFT JOIN clans cl ON cl.id = u.clan_id
      WHERE u.total_area_m2 > 0
      ORDER BY u.${orderCol} DESC
      LIMIT ${limit}
    `);

    const result = rows.map((r) => ({
      rank: Number(r.rank),
      user_id: r.user_id,
      username: r.username,
      avatar_url: r.avatar_url ?? null,
      color: r.color,
      city_name: r.city_name ?? null,
      clan_tag: r.clan_tag ?? null,
      total_area_m2: parseFloat(r.total_area_m2),
      territories_count: Number(r.territories_count),
      captures_count: Number(r.captures_count),
      distance_walked_m: parseFloat(r.distance_walked_m),
    }));

    await redis.setex(cacheKey, CACHE_TTL, JSON.stringify(result));
    res.json(result);
  } catch (err) {
    console.error('[Leaderboard] players error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /leaderboard/clans?sort=area&limit=50
router.get('/clans', requireAuth, async (req: Request, res: Response) => {
  const limit = Math.min(parseInt(req.query.limit as string) || 50, 100);
  const cacheKey = `leaderboard:clans:${limit}`;

  try {
    const cached = await redis.get(cacheKey);
    if (cached) {
      res.json(JSON.parse(cached));
      return;
    }

    const rows = await prisma.$queryRaw<any[]>`
      SELECT
        ROW_NUMBER() OVER (ORDER BY c.total_area_m2 DESC) AS rank,
        c.id AS clan_id,
        c.name,
        c.tag,
        c.color,
        c.total_area_m2,
        c.territories_count,
        COUNT(cm.user_id)::int AS members_count
      FROM clans c
      LEFT JOIN clan_members cm ON cm.clan_id = c.id
      GROUP BY c.id
      ORDER BY c.total_area_m2 DESC
      LIMIT ${limit}
    `;

    const result = rows.map((r) => ({
      rank: Number(r.rank),
      clan_id: r.clan_id,
      name: r.name,
      tag: r.tag,
      color: r.color,
      total_area_m2: parseFloat(r.total_area_m2),
      members_count: Number(r.members_count),
      territories_count: Number(r.territories_count),
    }));

    await redis.setex(cacheKey, CACHE_TTL, JSON.stringify(result));
    res.json(result);
  } catch (err) {
    console.error('[Leaderboard] clans error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export { router as leaderboardRouter };
