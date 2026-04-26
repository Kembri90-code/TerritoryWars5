import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';
import { requireAuth } from '../middleware/auth';

const router = Router();

// GET /api/achievements — все достижения
router.get('/', requireAuth, async (_req: Request, res: Response) => {
  try {
    const achievements = await prisma.achievement.findMany({
      orderBy: [{ category: 'asc' }, { points: 'asc' }],
    });
    res.json(achievements);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/achievements/me — разблокированные достижения текущего пользователя
router.get('/me', requireAuth, async (req: Request, res: Response) => {
  try {
    const userId = req.user!.userId;
    const unlocked = await prisma.userAchievement.findMany({
      where: { userId },
      include: { achievement: true },
      orderBy: { unlockedAt: 'desc' },
    });
    res.json(
      unlocked.map((ua) => ({
        id: ua.achievement.id,
        key: ua.achievement.key,
        name: ua.achievement.name,
        description: ua.achievement.description,
        category: ua.achievement.category,
        points: ua.achievement.points,
        icon: ua.achievement.icon,
        unlocked_at: ua.unlockedAt,
      }))
    );
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

export { router as achievementsRouter };
