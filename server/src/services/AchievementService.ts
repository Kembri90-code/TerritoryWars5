import { prisma } from '../prisma';

interface CheckContext {
  capturesCount?: number;
  takeoversCount?: number;
  totalAreaM2?: number;
  distanceWalkedM?: number;
  clanId?: string | null;
  isAbsorption?: boolean;
  isFigure8?: boolean;
  captureAreaM2?: number;
  captureDurationMs?: number;
  captureHour?: number;
}

export const AchievementService = {
  async checkAndGrant(userId: string, ctx: CheckContext): Promise<string[]> {
    const candidates: string[] = [];

    if (ctx.capturesCount !== undefined) {
      if (ctx.capturesCount >= 1)   candidates.push('ach_first_capture');
      if (ctx.capturesCount >= 10)  candidates.push('ach_captures_10');
      if (ctx.capturesCount >= 50)  candidates.push('ach_captures_50');
      if (ctx.capturesCount >= 200) candidates.push('ach_captures_200');
    }

    if (ctx.totalAreaM2 !== undefined) {
      if (ctx.totalAreaM2 >= 10_000)     candidates.push('ach_area_10k');
      if (ctx.totalAreaM2 >= 1_000_000)  candidates.push('ach_area_1km');
      if (ctx.totalAreaM2 >= 10_000_000) candidates.push('ach_area_10km');
    }

    if (ctx.takeoversCount !== undefined) {
      if (ctx.takeoversCount >= 1)  candidates.push('ach_first_takeover');
      if (ctx.takeoversCount >= 50) candidates.push('ach_takeover_50');
    }

    if (ctx.distanceWalkedM !== undefined) {
      if (ctx.distanceWalkedM >= 1_000)   candidates.push('ach_walk_1km');
      if (ctx.distanceWalkedM >= 42_000)  candidates.push('ach_walk_42km');
      if (ctx.distanceWalkedM >= 100_000) candidates.push('ach_walk_100km');
      if (ctx.distanceWalkedM >= 500_000) candidates.push('ach_walk_500km');
    }

    if (ctx.clanId) candidates.push('ach_join_clan');
    if (ctx.isAbsorption) candidates.push('ach_absorb');
    if (ctx.isFigure8)    candidates.push('ach_figure8');

    if (ctx.captureAreaM2 !== undefined && ctx.captureAreaM2 >= 5_000)
      candidates.push('ach_large_capture');

    if (ctx.captureDurationMs !== undefined && ctx.captureDurationMs <= 5 * 60 * 1000)
      candidates.push('ach_fast_capture');

    if (ctx.captureHour !== undefined) {
      if (ctx.captureHour < 7)  candidates.push('ach_early_bird');
      if (ctx.captureHour >= 23) candidates.push('ach_night_hunter');
    }

    if (candidates.length === 0) return [];

    const existing = await prisma.userAchievement.findMany({
      where: { userId, achievementId: { in: candidates } },
      select: { achievementId: true },
    });
    const existingSet = new Set(existing.map((e) => e.achievementId));
    const toGrant = candidates.filter((id) => !existingSet.has(id));

    if (toGrant.length === 0) return [];

    await prisma.userAchievement.createMany({
      data: toGrant.map((achievementId) => ({ userId, achievementId })),
      skipDuplicates: true,
    });

    return toGrant;
  },
};
