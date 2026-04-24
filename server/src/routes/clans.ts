import { Router, Request, Response } from 'express';
import { z } from 'zod';
import path from 'path';
import fs from 'fs';
import { prisma } from '../prisma';
import { requireAuth } from '../middleware/auth';
import { uploadAvatar } from '../middleware/upload';
import { config } from '../config';
import { NotificationService } from '../services/NotificationService';

const router = Router();

const CreateClanSchema = z.object({
  name: z.string().min(3).max(50),
  tag: z.string().min(2).max(4).toUpperCase(),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/),
  description: z.string().max(200).optional().nullable(),
});

function formatClan(clan: any, membersCount?: number) {
  return {
    id: clan.id,
    name: clan.name,
    tag: clan.tag,
    leader_id: clan.leaderId,
    color: clan.color,
    avatar_url: clan.avatarUrl ?? null,
    description: clan.description ?? null,
    total_area_m2: clan.totalAreaM2,
    members_count: membersCount ?? clan._count?.members ?? clan.members?.length ?? 0,
    max_members: clan.maxMembers,
    created_at: clan.createdAt instanceof Date ? clan.createdAt.toISOString() : String(clan.createdAt),
  };
}

function formatMember(m: any) {
  return {
    user_id: m.userId,
    username: m.user?.username ?? '',
    avatar_url: m.user?.avatarUrl ?? null,
    color: m.user?.color ?? '#2979FF',
    role: m.role,
    total_area_m2: m.user?.totalAreaM2 ?? 0,
    joined_at: m.joinedAt instanceof Date ? m.joinedAt.toISOString() : String(m.joinedAt),
  };
}

const memberInclude = {
  user: { select: { username: true, avatarUrl: true, color: true, totalAreaM2: true } },
};

// POST /clans
router.post('/', requireAuth, async (req: Request, res: Response) => {
  const parsed = CreateClanSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation error', details: parsed.error.flatten() });
    return;
  }
  const { name, tag, color, description } = parsed.data;
  const userId = req.user!.userId;

  try {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }
    if (user.clanId) { res.status(409).json({ error: 'You are already in a clan' }); return; }

    const [nameExists, tagExists] = await Promise.all([
      prisma.clan.findUnique({ where: { name } }),
      prisma.clan.findUnique({ where: { tag } }),
    ]);
    if (nameExists || tagExists) {
      res.status(409).json({ error: 'Clan name or tag already taken' });
      return;
    }

    const clan = await prisma.$transaction(async (tx) => {
      const newClan = await tx.clan.create({
        data: { name, tag, color, description: description ?? null, leaderId: userId },
      });
      await tx.clanMember.create({
        data: { userId, clanId: newClan.id, role: 'LEADER' },
      });
      await tx.user.update({
        where: { id: userId },
        data: { clanId: newClan.id },
      });
      return newClan;
    });

    res.status(201).json(formatClan(clan, 1));
  } catch (err) {
    console.error('[Clans] create error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /clans/:id
router.get('/:id', requireAuth, async (req: Request, res: Response) => {
  try {
    const clan = await prisma.clan.findUnique({
      where: { id: req.params.id },
      include: { _count: { select: { members: true } } },
    });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    res.json(formatClan(clan));
  } catch (err) {
    console.error('[Clans] getById error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /clans/:id
router.put('/:id', requireAuth, async (req: Request, res: Response) => {
  const parsed = CreateClanSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: 'Validation error', details: parsed.error.flatten() });
    return;
  }
  try {
    const clan = await prisma.clan.findUnique({ where: { id: req.params.id } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan.leaderId !== req.user!.userId) { res.status(403).json({ error: 'Only the clan leader can edit' }); return; }

    const updated = await prisma.clan.update({
      where: { id: req.params.id },
      data: {
        name: parsed.data.name,
        tag: parsed.data.tag,
        color: parsed.data.color,
        description: parsed.data.description ?? null,
      },
      include: { _count: { select: { members: true } } },
    });
    res.json(formatClan(updated));
  } catch (err: any) {
    if (err.code === 'P2002') { res.status(409).json({ error: 'Name or tag already taken' }); return; }
    console.error('[Clans] update error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /clans/:id/avatar — leader uploads clan avatar
router.post('/:id/avatar', requireAuth, (req: Request, res: Response) => {
  uploadAvatar(req, res, async (err) => {
    if (err) { res.status(400).json({ error: err.message }); return; }
    if (!req.file) { res.status(400).json({ error: 'No file uploaded' }); return; }
    try {
      const clan = await prisma.clan.findUnique({ where: { id: req.params.id } });
      if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
      if (clan.leaderId !== req.user!.userId) { res.status(403).json({ error: 'Only the clan leader can upload avatar' }); return; }
      // Delete old avatar if local
      if (clan.avatarUrl && !clan.avatarUrl.startsWith('http')) {
        const oldPath = path.join(config.uploads.dir, path.basename(clan.avatarUrl));
        if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
      }
      const avatarUrl = `/uploads/${req.file.filename}`;
      const updated = await prisma.clan.update({
        where: { id: req.params.id },
        data: { avatarUrl },
        include: { _count: { select: { members: true } } },
      });
      res.json(formatClan(updated));
    } catch (dbErr) {
      console.error('[Clans] uploadAvatar error:', dbErr);
      res.status(500).json({ error: 'Internal server error' });
    }
  });
});

// GET /clans/:id/members
router.get('/:id/members', requireAuth, async (req: Request, res: Response) => {
  try {
    const members = await prisma.clanMember.findMany({
      where: { clanId: req.params.id },
      include: memberInclude,
      orderBy: [{ role: 'asc' }, { joinedAt: 'asc' }],
    });
    res.json(members.map(formatMember));
  } catch (err) {
    console.error('[Clans] getMembers error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /clans/:id/join
router.post('/:id/join', requireAuth, async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const clanId = req.params.id;

  try {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }
    if (user.clanId) { res.status(409).json({ error: 'You are already in a clan' }); return; }

    const clan = await prisma.clan.findUnique({
      where: { id: clanId },
      include: { _count: { select: { members: true } } },
    });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan._count.members >= clan.maxMembers) {
      res.status(403).json({ error: 'Clan is full' });
      return;
    }

    await prisma.$transaction(async (tx) => {
      await tx.clanMember.create({ data: { userId, clanId, role: 'MEMBER' } });
      await tx.user.update({ where: { id: userId }, data: { clanId } });
      // Update clan territory area to include user's territories
      await tx.$executeRaw`
        UPDATE clans
        SET total_area_m2 = (
          SELECT COALESCE(SUM(area_m2), 0) FROM territories WHERE clan_id::text = ${clanId}
        ),
        territories_count = (
          SELECT COUNT(*) FROM territories WHERE clan_id::text = ${clanId}
        )
        WHERE id::text = ${clanId}
      `;
    });

    res.status(204).send();
  } catch (err) {
    console.error('[Clans] join error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /clans/:id/leave
router.post('/:id/leave', requireAuth, async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const clanId = req.params.id;

  try {
    const clan = await prisma.clan.findUnique({ where: { id: clanId } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }

    const membership = await prisma.clanMember.findUnique({
      where: { userId },
    });
    if (!membership) { res.status(404).json({ error: 'You are not in this clan' }); return; }

    if (clan.leaderId === userId) {
      // Leader leaving: transfer leadership or disband
      const nextLeader = await prisma.clanMember.findFirst({
        where: { clanId, userId: { not: userId } },
        orderBy: { joinedAt: 'asc' },
      });

      if (!nextLeader) {
        // Disband clan
        await prisma.$transaction(async (tx) => {
          await tx.user.updateMany({ where: { clanId }, data: { clanId: null } });
          await tx.clan.delete({ where: { id: clanId } });
        });
        res.status(204).send();
        return;
      }

      // Transfer to oldest member
      await prisma.$transaction(async (tx) => {
        await tx.clan.update({ where: { id: clanId }, data: { leaderId: nextLeader.userId } });
        await tx.clanMember.update({
          where: { userId: nextLeader.userId },
          data: { role: 'LEADER' },
        });
        await tx.clanMember.delete({ where: { userId } });
        await tx.user.update({ where: { id: userId }, data: { clanId: null } });
      });
    } else {
      await prisma.$transaction(async (tx) => {
        await tx.clanMember.delete({ where: { userId } });
        await tx.user.update({ where: { id: userId }, data: { clanId: null } });
      });
    }

    res.status(204).send();
  } catch (err) {
    console.error('[Clans] leave error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /clans/:id/members/:userId  (kick)
router.delete('/:id/members/:userId', requireAuth, async (req: Request, res: Response) => {
  const requesterId = req.user!.userId;
  const clanId = req.params.id;
  const targetId = req.params.userId;

  try {
    const clan = await prisma.clan.findUnique({ where: { id: clanId } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }

    // Only leader or officer can kick (leader always can; officer cannot kick leader/officer)
    const requesterMembership = await prisma.clanMember.findUnique({
      where: { userId: requesterId },
    });
    if (!requesterMembership || requesterMembership.role === 'MEMBER') {
      res.status(403).json({ error: 'Insufficient permissions' });
      return;
    }

    const targetMembership = await prisma.clanMember.findUnique({
      where: { userId: targetId },
    });
    if (!targetMembership) { res.status(404).json({ error: 'Member not found' }); return; }
    if (targetMembership.role === 'LEADER') { res.status(403).json({ error: 'Cannot kick the leader' }); return; }
    if (requesterMembership.role === 'OFFICER' && targetMembership.role === 'OFFICER') {
      res.status(403).json({ error: 'Officers cannot kick other officers' });
      return;
    }

    await prisma.$transaction(async (tx) => {
      await tx.clanMember.delete({ where: { userId: targetId } });
      await tx.user.update({ where: { id: targetId }, data: { clanId: null } });
    });

    res.status(204).send();
  } catch (err) {
    console.error('[Clans] kick error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /clans/:id — leader deletes the clan; all members lose their clan
router.delete('/:id', requireAuth, async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const clanId = req.params.id;

  try {
    const clan = await prisma.clan.findUnique({ where: { id: clanId } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan.leaderId !== userId) { res.status(403).json({ error: 'Only the clan leader can delete the clan' }); return; }

    await prisma.$transaction(async (tx) => {
      // Be explicit with cleanup: production DB constraints may differ from Prisma schema
      // (e.g. missing ON DELETE CASCADE/SET NULL on legacy migrations).
      await tx.user.updateMany({ where: { clanId }, data: { clanId: null } });
      await tx.$executeRaw`UPDATE territories SET clan_id = NULL WHERE clan_id::text = ${clanId}`;
      await tx.clanJoinRequest.deleteMany({ where: { clanId } });
      await tx.clanMember.deleteMany({ where: { clanId } });
      await tx.clan.delete({ where: { id: clanId } });
    });

    res.status(204).send();
  } catch (err) {
    console.error('[Clans] delete error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /clans/:id/territories
router.get('/:id/territories', requireAuth, async (req: Request, res: Response) => {
  try {
    const rows = await prisma.$queryRaw<any[]>`
      SELECT
        t.id, t.owner_id, t.clan_id, t.area_m2, t.perimeter_m,
        t.captured_at, t.updated_at,
        ST_AsGeoJSON(t.polygon)::json AS geojson,
        u.username AS owner_username, u.color AS owner_color,
        c.color AS clan_color
      FROM territories t
      JOIN users u ON u.id = t.owner_id
      LEFT JOIN clans c ON c.id = t.clan_id
      WHERE t.clan_id = ${req.params.id}
    `;

    const territories = rows.map((row) => {
      const coords: [number, number][] = row.geojson?.coordinates?.[0] ?? [];
      return {
        id: row.id,
        owner_id: row.owner_id,
        owner_username: row.owner_username,
        owner_color: row.owner_color,
        clan_id: row.clan_id ?? null,
        clan_color: row.clan_color ?? null,
        polygon: coords.map(([lng, lat]: [number, number]) => [lng, lat]),
        area_m2: parseFloat(row.area_m2),
        perimeter_m: parseFloat(row.perimeter_m),
        captured_at: row.captured_at instanceof Date ? row.captured_at.toISOString() : String(row.captured_at),
        updated_at: row.updated_at instanceof Date ? row.updated_at.toISOString() : String(row.updated_at),
      };
    });

    res.json(territories);
  } catch (err) {
    console.error('[Clans] getTerritories error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /clans/:id/activity — recent territory captures in the clan
router.get('/:id/activity', requireAuth, async (req: Request, res: Response) => {
  const clanId = req.params.id;
  const limit = Math.min(parseInt(req.query.limit as string) || 30, 100);
  try {
    const rows = await prisma.$queryRaw<any[]>`
      SELECT
        t.id, t.owner_id, t.area_m2, t.captured_at, t.updated_at,
        u.username AS owner_username, u.color AS owner_color
      FROM territories t
      JOIN users u ON u.id = t.owner_id
      WHERE t.clan_id::text = ${clanId}
      ORDER BY t.captured_at DESC
      LIMIT ${limit}
    `;
    res.json(rows.map((r) => ({
      territory_id: r.id,
      owner_id: r.owner_id,
      owner_username: r.owner_username,
      owner_color: r.owner_color,
      area_m2: parseFloat(r.area_m2),
      captured_at: r.captured_at instanceof Date ? r.captured_at.toISOString() : String(r.captured_at),
    })));
  } catch (err) {
    console.error('[Clans] getActivity error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /clans/:id/request — player sends join request
router.post('/:id/request', requireAuth, async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const clanId = req.params.id;
  try {
    const [user, clan] = await Promise.all([
      prisma.user.findUnique({ where: { id: userId } }),
      prisma.clan.findUnique({ where: { id: clanId }, include: { _count: { select: { members: true } } } }),
    ]);
    if (!user) { res.status(404).json({ error: 'User not found' }); return; }
    if (user.clanId) { res.status(409).json({ error: 'You are already in a clan' }); return; }
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan._count.members >= clan.maxMembers) { res.status(403).json({ error: 'Clan is full' }); return; }

    const existing = await prisma.clanJoinRequest.findUnique({
      where: { userId_clanId: { userId, clanId } },
    });
    if (existing) { res.status(409).json({ error: 'Request already sent' }); return; }

    await prisma.clanJoinRequest.create({ data: { userId, clanId } });
    await NotificationService.notifyClanJoinRequest(clan.leaderId, user.username, clanId, clan.name);

    res.status(201).json({ success: true });
  } catch (err) {
    console.error('[Clans] request error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /clans/:id/requests — pending requests (leader only)
router.get('/:id/requests', requireAuth, async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const clanId = req.params.id;
  try {
    const clan = await prisma.clan.findUnique({ where: { id: clanId } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan.leaderId !== userId) { res.status(403).json({ error: 'Only the clan leader can view requests' }); return; }

    const requests = await prisma.clanJoinRequest.findMany({
      where: { clanId },
      include: {
        user: { select: { id: true, username: true, avatarUrl: true, color: true, totalAreaM2: true } },
      },
      orderBy: { createdAt: 'asc' },
    });

    res.json(requests.map((r) => ({
      user_id: r.user.id,
      username: r.user.username,
      avatar_url: r.user.avatarUrl ?? null,
      color: r.user.color,
      total_area_m2: r.user.totalAreaM2,
      created_at: r.createdAt.toISOString(),
    })));
  } catch (err) {
    console.error('[Clans] getRequests error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /clans/:id/requests/:userId/accept — leader accepts request
router.post('/:id/requests/:userId/accept', requireAuth, async (req: Request, res: Response) => {
  const leaderId = req.user!.userId;
  const clanId = req.params.id;
  const applicantId = req.params.userId;
  try {
    const clan = await prisma.clan.findUnique({
      where: { id: clanId },
      include: { _count: { select: { members: true } } },
    });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan.leaderId !== leaderId) { res.status(403).json({ error: 'Only the clan leader can accept requests' }); return; }
    if (clan._count.members >= clan.maxMembers) { res.status(403).json({ error: 'Clan is full' }); return; }

    const applicant = await prisma.user.findUnique({ where: { id: applicantId } });
    if (!applicant) { res.status(404).json({ error: 'User not found' }); return; }
    if (applicant.clanId) {
      await prisma.clanJoinRequest.deleteMany({ where: { userId: applicantId, clanId } });
      res.status(409).json({ error: 'User is already in another clan' }); return;
    }

    await prisma.$transaction(async (tx) => {
      await tx.clanMember.create({ data: { userId: applicantId, clanId, role: 'MEMBER' } });
      await tx.user.update({ where: { id: applicantId }, data: { clanId } });
      // Assign all existing territories of the new member to the clan
      await tx.$executeRaw`UPDATE territories SET clan_id = ${clanId}::uuid WHERE owner_id::text = ${applicantId}`;
      // Remove this and any other pending requests from this user
      await tx.clanJoinRequest.deleteMany({ where: { userId: applicantId } });
      // Recalculate clan stats
      await tx.$executeRaw`
        UPDATE clans
        SET total_area_m2     = (SELECT COALESCE(SUM(area_m2), 0) FROM territories WHERE clan_id::text = ${clanId}),
            territories_count = (SELECT COUNT(*)                  FROM territories WHERE clan_id::text = ${clanId})
        WHERE id::text = ${clanId}
      `;
    });

    res.status(204).send();
  } catch (err) {
    console.error('[Clans] acceptRequest error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /clans/:id/requests/:userId — leader declines request
router.delete('/:id/requests/:userId', requireAuth, async (req: Request, res: Response) => {
  const leaderId = req.user!.userId;
  const clanId = req.params.id;
  const applicantId = req.params.userId;
  try {
    const clan = await prisma.clan.findUnique({ where: { id: clanId } });
    if (!clan) { res.status(404).json({ error: 'Clan not found' }); return; }
    if (clan.leaderId !== leaderId) { res.status(403).json({ error: 'Only the clan leader can decline requests' }); return; }

    await prisma.clanJoinRequest.deleteMany({ where: { userId: applicantId, clanId } });
    res.status(204).send();
  } catch (err) {
    console.error('[Clans] declineRequest error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export { router as clansRouter };
