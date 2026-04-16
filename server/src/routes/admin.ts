import { Router, Request, Response } from 'express';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { prisma } from '../prisma';
import { config } from '../config';
import { requireAdmin } from '../middleware/adminAuth';

const router = Router();

const LoginSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
});

// POST /admin/login
router.post('/login', async (req: Request, res: Response) => {
  const parsed = LoginSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Invalid request' });
    return;
  }

  const { username, password } = parsed.data;

  try {
    const admin = await prisma.admin.findUnique({ where: { username } });
    if (!admin) {
      res.status(401).json({ error: 'Invalid credentials' });
      return;
    }

    const valid = await bcrypt.compare(password, admin.passwordHash);
    if (!valid) {
      res.status(401).json({ error: 'Invalid credentials' });
      return;
    }

    const token = jwt.sign(
      { adminId: admin.id, username: admin.username, role: 'admin' },
      config.jwt.accessSecret,
      { expiresIn: '8h' }
    );

    res.json({ token, admin: { id: admin.id, username: admin.username } });
  } catch (err) {
    console.error('[Admin] login error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /admin/stats
router.get('/stats', requireAdmin, async (_req: Request, res: Response) => {
  try {
    const [usersCount, territoriesCount, clansCount, activeSessions] = await Promise.all([
      prisma.user.count(),
      prisma.territory.count(),
      prisma.clan.count(),
      prisma.refreshToken.count({ where: { expiresAt: { gt: new Date() } } }),
    ]);

    const [totalAreaResult] = await prisma.$queryRaw<{ total: number }[]>`
      SELECT COALESCE(SUM(area_m2), 0)::float AS total FROM territories
    `;

    const topUsers = await prisma.user.findMany({
      orderBy: { totalAreaM2: 'desc' },
      take: 5,
      select: { id: true, username: true, totalAreaM2: true, territoriesCount: true, color: true },
    });

    const topClans = await prisma.clan.findMany({
      orderBy: { totalAreaM2: 'desc' },
      take: 5,
      select: { id: true, name: true, tag: true, totalAreaM2: true, territoriesCount: true, color: true },
    });

    res.json({
      users_count: usersCount,
      territories_count: territoriesCount,
      clans_count: clansCount,
      active_sessions: activeSessions,
      total_area_m2: totalAreaResult?.total ?? 0,
      top_users: topUsers.map(u => ({
        id: u.id,
        username: u.username,
        total_area_m2: u.totalAreaM2,
        territories_count: u.territoriesCount,
        color: u.color,
      })),
      top_clans: topClans.map(c => ({
        id: c.id,
        name: c.name,
        tag: c.tag,
        total_area_m2: c.totalAreaM2,
        territories_count: c.territoriesCount,
        color: c.color,
      })),
    });
  } catch (err) {
    console.error('[Admin] stats error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /admin/users?page=1&limit=20&search=
router.get('/users', requireAdmin, async (req: Request, res: Response) => {
  const page = Math.max(1, parseInt(String(req.query.page || '1'), 10));
  const limit = Math.min(100, Math.max(1, parseInt(String(req.query.limit || '20'), 10)));
  const search = String(req.query.search || '').trim();
  const skip = (page - 1) * limit;

  try {
    const where = search
      ? { OR: [{ username: { contains: search, mode: 'insensitive' as const } }, { email: { contains: search, mode: 'insensitive' as const } }] }
      : {};

    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where,
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          username: true,
          email: true,
          color: true,
          avatarUrl: true,
          totalAreaM2: true,
          territoriesCount: true,
          capturesCount: true,
          createdAt: true,
          city: { select: { name: true, region: true } },
          clan: { select: { id: true, name: true, tag: true } },
        },
      }),
      prisma.user.count({ where }),
    ]);

    res.json({
      users: users.map(u => ({
        id: u.id,
        username: u.username,
        email: u.email,
        color: u.color,
        avatar_url: u.avatarUrl,
        total_area_m2: u.totalAreaM2,
        territories_count: u.territoriesCount,
        captures_count: u.capturesCount,
        created_at: u.createdAt.toISOString(),
        city: u.city ? `${u.city.name}, ${u.city.region}` : null,
        clan: u.clan ? { id: (u.clan as any).id, name: (u.clan as any).name, tag: (u.clan as any).tag } : null,
      })),
      pagination: { page, limit, total, pages: Math.ceil(total / limit) },
    });
  } catch (err) {
    console.error('[Admin] users list error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /admin/users/:id
router.get('/users/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.params.id },
      include: {
        city: true,
        clan: { select: { id: true, name: true, tag: true } },
        _count: { select: { territories: true, refreshTokens: true } },
      },
    });
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }

    res.json({
      id: user.id,
      username: user.username,
      email: user.email,
      color: user.color,
      avatar_url: user.avatarUrl,
      total_area_m2: user.totalAreaM2,
      territories_count: user.territoriesCount,
      captures_count: user.capturesCount,
      takeovers_count: user.takeoversCount,
      distance_walked_m: user.distanceWalkedM,
      created_at: user.createdAt.toISOString(),
      updated_at: user.updatedAt.toISOString(),
      city: { id: user.city.id, name: user.city.name, region: user.city.region },
      clan: user.clan ? { id: (user.clan as any).id, name: (user.clan as any).name, tag: (user.clan as any).tag } : null,
      active_sessions: user._count.refreshTokens,
    });
  } catch (err) {
    console.error('[Admin] getUser error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /admin/users/:id
router.delete('/users/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const user = await prisma.user.findUnique({ where: { id: req.params.id } });
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }

    await prisma.user.delete({ where: { id: req.params.id } });
    res.status(204).send();
  } catch (err) {
    console.error('[Admin] deleteUser error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /admin/users/:id/reset-sessions  — revoke all refresh tokens
router.post('/users/:id/reset-sessions', requireAdmin, async (req: Request, res: Response) => {
  try {
    await prisma.refreshToken.deleteMany({ where: { userId: req.params.id } });
    res.status(204).send();
  } catch (err) {
    console.error('[Admin] resetSessions error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /admin/clans?page=1&limit=20&search=
router.get('/clans', requireAdmin, async (req: Request, res: Response) => {
  const page = Math.max(1, parseInt(String(req.query.page || '1'), 10));
  const limit = Math.min(100, Math.max(1, parseInt(String(req.query.limit || '20'), 10)));
  const search = String(req.query.search || '').trim();
  const skip = (page - 1) * limit;

  try {
    const where = search
      ? { OR: [{ name: { contains: search, mode: 'insensitive' as const } }, { tag: { contains: search, mode: 'insensitive' as const } }] }
      : {};

    const [clans, total] = await Promise.all([
      prisma.clan.findMany({
        where,
        skip,
        take: limit,
        orderBy: { totalAreaM2: 'desc' },
        include: {
          leader: { select: { username: true } },
          _count: { select: { members: true } },
        },
      }),
      prisma.clan.count({ where }),
    ]);

    res.json({
      clans: clans.map(c => ({
        id: c.id,
        name: c.name,
        tag: c.tag,
        color: c.color,
        description: c.description,
        leader_username: c.leader.username,
        members_count: c._count.members,
        max_members: c.maxMembers,
        total_area_m2: c.totalAreaM2,
        territories_count: c.territoriesCount,
        created_at: c.createdAt.toISOString(),
      })),
      pagination: { page, limit, total, pages: Math.ceil(total / limit) },
    });
  } catch (err) {
    console.error('[Admin] clans list error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /admin/clans/:id
router.delete('/clans/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const clan = await prisma.clan.findUnique({ where: { id: req.params.id } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }

    await prisma.$transaction(async (tx) => {
      await tx.user.updateMany({ where: { clanId: req.params.id }, data: { clanId: null } });
      await tx.clan.delete({ where: { id: req.params.id } });
    });

    res.status(204).send();
  } catch (err) {
    console.error('[Admin] deleteClan error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /admin/territories?page=1&limit=20
router.get('/territories', requireAdmin, async (req: Request, res: Response) => {
  const page = Math.max(1, parseInt(String(req.query.page || '1'), 10));
  const limit = Math.min(100, Math.max(1, parseInt(String(req.query.limit || '20'), 10)));
  const skip = (page - 1) * limit;

  try {
    const [rows, total] = await Promise.all([
      prisma.$queryRaw<any[]>`
        SELECT
          t.id, t.owner_id, t.clan_id, t.area_m2, t.perimeter_m,
          t.captured_at, t.updated_at,
          u.username AS owner_username, u.color AS owner_color,
          c.name AS clan_name, c.tag AS clan_tag
        FROM territories t
        JOIN users u ON u.id = t.owner_id
        LEFT JOIN clans c ON c.id = t.clan_id
        ORDER BY t.area_m2 DESC
        LIMIT ${limit} OFFSET ${skip}
      `,
      prisma.territory.count(),
    ]);

    res.json({
      territories: rows.map(r => ({
        id: r.id,
        owner_id: r.owner_id,
        owner_username: r.owner_username,
        owner_color: r.owner_color,
        clan_id: r.clan_id ?? null,
        clan_name: r.clan_name ?? null,
        clan_tag: r.clan_tag ?? null,
        area_m2: parseFloat(r.area_m2),
        perimeter_m: parseFloat(r.perimeter_m),
        captured_at: r.captured_at instanceof Date ? r.captured_at.toISOString() : String(r.captured_at),
      })),
      pagination: { page, limit, total, pages: Math.ceil(total / limit) },
    });
  } catch (err) {
    console.error('[Admin] territories list error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /admin/territories/:id
router.delete('/territories/:id', requireAdmin, async (req: Request, res: Response) => {
  try {
    const territory = await prisma.territory.findUnique({ where: { id: req.params.id } });
    if (!territory) { res.status(404).json({ error: 'Territory not found' }); return; }

    await prisma.$transaction(async (tx) => {
      await tx.territory.delete({ where: { id: req.params.id } });

      // Recalculate owner stats
      const [agg] = await tx.$queryRaw<{ area: number; cnt: bigint }[]>`
        SELECT COALESCE(SUM(area_m2), 0)::float AS area, COUNT(*)::bigint AS cnt
        FROM territories WHERE owner_id = ${territory.ownerId}
      `;
      await tx.user.update({
        where: { id: territory.ownerId },
        data: { totalAreaM2: agg.area, territoriesCount: Number(agg.cnt) },
      });

      // Recalculate clan stats if applicable
      if (territory.clanId) {
        const [clanAgg] = await tx.$queryRaw<{ area: number; cnt: bigint }[]>`
          SELECT COALESCE(SUM(area_m2), 0)::float AS area, COUNT(*)::bigint AS cnt
          FROM territories WHERE clan_id = ${territory.clanId}
        `;
        await tx.clan.update({
          where: { id: territory.clanId },
          data: { totalAreaM2: clanAgg.area, territoriesCount: Number(clanAgg.cnt) },
        });
      }
    });

    res.status(204).send();
  } catch (err) {
    console.error('[Admin] deleteTerritory error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export { router as adminRouter };
