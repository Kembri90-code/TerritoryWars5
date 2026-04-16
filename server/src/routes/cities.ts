import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';

const router = Router();

// GET /cities?search=...
router.get('/', async (req: Request, res: Response) => {
  const search = (req.query.search as string | undefined)?.trim();

  try {
    const cities = await prisma.city.findMany({
      where: search
        ? { name: { contains: search, mode: 'insensitive' } }
        : undefined,
      orderBy: { population: 'desc' },
      take: 50,
    });

    res.json(
      cities.map((c) => ({
        id: c.id,
        name: c.name,
        region: c.region,
        population: c.population,
        lat: c.lat,
        lng: c.lng,
      }))
    );
  } catch (err) {
    console.error('[Cities] error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

export { router as citiesRouter };
