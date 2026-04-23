import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { requireAuth } from '../middleware/auth';
import { emitTerritoryDeleted, emitTerritoryUpdate } from '../ws/socket';

const router = Router();

const RoutePointSchema = z.object({
  lat: z.number().min(-90).max(90),
  lng: z.number().min(-180).max(180),
  timestamp: z.number().int(),
  accuracy: z.number().min(0),
});

const CaptureSchema = z.object({
  route_points: z.array(RoutePointSchema).min(4),
});

function formatTerritory(t: any) {
  return {
    id: t.id,
    owner_id: t.ownerId,
    owner_username: t.owner?.username ?? '',
    owner_color: t.owner?.color ?? '#2979FF',
    clan_id: t.clanId ?? null,
    clan_color: t.clan?.color ?? null,
    polygon: t.polygon ?? [],
    area_m2: t.areaM2,
    perimeter_m: t.perimeterM,
    captured_at: t.capturedAt.toISOString(),
    updated_at: t.updatedAt.toISOString(),
  };
}

// GET /territories?bbox=lat1,lng1,lat2,lng2
router.get('/', requireAuth, async (req: Request, res: Response) => {
  const bbox = req.query.bbox as string;
  if (!bbox) { res.status(400).json({ error: 'Missing bbox parameter' }); return; }

  const parts = bbox.split(',').map(Number);
  if (parts.length !== 4 || parts.some(isNaN)) {
    res.status(400).json({ error: 'Invalid bbox format. Expected: lat1,lng1,lat2,lng2' });
    return;
  }
  const [lat1, lng1, lat2, lng2] = parts;
  const minLat = Math.min(lat1, lat2);
  const maxLat = Math.max(lat1, lat2);
  const minLng = Math.min(lng1, lng2);
  const maxLng = Math.max(lng1, lng2);

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
      WHERE t.polygon && ST_MakeEnvelope(${minLng}, ${minLat}, ${maxLng}, ${maxLat}, 4326)
      LIMIT 500
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
        polygon: coords.map(([lng, lat]) => [lng, lat]),
        area_m2: row.area_m2,
        perimeter_m: row.perimeter_m,
        captured_at: row.captured_at.toISOString(),
        updated_at: row.updated_at.toISOString(),
      };
    });

    res.json(territories);
  } catch (err) {
    console.error('[Territories] getBbox error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /territories/my — must be before /:id
router.get('/my', requireAuth, async (req: Request, res: Response) => {
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
      WHERE t.owner_id = ${req.user!.userId}
    `;
    res.json(rows.map(rowToTerritory));
  } catch (err) {
    console.error('[Territories] getMy error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /territories/:id
router.get('/:id', requireAuth, async (req: Request, res: Response) => {
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
      WHERE t.id = ${req.params.id}
    `;
    if (rows.length === 0) { res.status(404).json({ error: 'Territory not found' }); return; }
    res.json(rowToTerritory(rows[0]));
  } catch (err) {
    console.error('[Territories] getById error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /territories/capture
router.post('/capture', requireAuth, async (req: Request, res: Response) => {
  const parsed = CaptureSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ success: false, error: 'Invalid route data', territory: null, merged: false });
    return;
  }

  const { route_points } = parsed.data;
  const userId = req.user!.userId;

  // Anti-cheat: check speed between points
  for (let i = 1; i < route_points.length; i++) {
    const prev = route_points[i - 1];
    const curr = route_points[i];
    const dt = (curr.timestamp - prev.timestamp) / 1000; // seconds
    if (dt <= 0) continue;
    const dist = haversineM(prev.lat, prev.lng, curr.lat, curr.lng);
    const speed = dist / dt; // m/s
    if (speed > 12) { // ~43 km/h — impossible on foot
      res.status(400).json({ success: false, error: 'Anti-cheat: speed too high', territory: null, merged: false });
      return;
    }
  }

  // Build closed ring — close polygon if not closed
  const points = [...route_points];
  const first = points[0];
  const last = points[points.length - 1];
  if (first.lat !== last.lat || first.lng !== last.lng) {
    points.push(first);
  }

  if (points.length < 4) {
    res.status(400).json({ success: false, error: 'Polygon must have at least 3 unique points', territory: null, merged: false });
    return;
  }

  // Build WKT polygon
  const wkt = `POLYGON((${points.map((p) => `${p.lng} ${p.lat}`).join(', ')}))`;

  try {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) { res.status(404).json({ success: false, error: 'User not found', territory: null, merged: false }); return; }

    // Validate polygon and compute metrics via PostGIS
    const [validity] = await prisma.$queryRaw<any[]>`
      SELECT
        ST_IsValid(ST_GeomFromText(${wkt}, 4326)) AS valid,
        ST_Area(ST_GeomFromText(${wkt}, 4326)::geography) AS area_m2,
        ST_Perimeter(ST_GeomFromText(${wkt}, 4326)::geography) AS perimeter_m
    `;

    if (!validity.valid) {
      res.status(400).json({ success: false, error: 'Invalid polygon geometry', territory: null, merged: false });
      return;
    }

    const areaM2: number = parseFloat(validity.area_m2);
    const perimeterM: number = parseFloat(validity.perimeter_m);

    // Min area: 100 m², max: 5 km²
    if (areaM2 < 100) {
      res.status(400).json({ success: false, error: 'Territory too small (min 100 m²)', territory: null, merged: false });
      return;
    }
    if (areaM2 > 5_000_000) {
      res.status(400).json({ success: false, error: 'Territory too large (max 5 km²)', territory: null, merged: false });
      return;
    }

    // Find all enemy territories that overlap with the new polygon
    const enemyOverlaps = await prisma.$queryRaw<any[]>`
      SELECT t.id, t.owner_id, t.clan_id, t.area_m2::float AS area_m2
      FROM territories t
      WHERE t.owner_id != ${userId}
        AND ST_Intersects(t.polygon, ST_GeomFromText(${wkt}, 4326))
    `;

    let conqueredCount = 0;

    for (const enemy of enemyOverlaps) {
      const oldArea: number = parseFloat(enemy.area_m2);

      // Get the largest remaining piece of enemy territory after clipping
      const pieces = await prisma.$queryRaw<any[]>`
        SELECT
          ST_AsText((dp).geom)                   AS piece_wkt,
          ST_Area((dp).geom::geography)           AS piece_area,
          ST_Perimeter((dp).geom::geography)      AS piece_perimeter
        FROM (
          SELECT ST_Dump(
            ST_MakeValid(ST_Difference(
              (SELECT polygon FROM territories WHERE id = ${enemy.id}),
              ST_GeomFromText(${wkt}, 4326)
            ))
          ) AS dp
        ) AS dumped
        WHERE ST_GeometryType((dp).geom) = 'ST_Polygon'
          AND ST_Area((dp).geom::geography) >= 100
        ORDER BY piece_area DESC
        LIMIT 1
      `;

      conqueredCount++;

      if (pieces.length === 0) {
        // Full capture: no valid remainder — delete enemy territory
        await prisma.$executeRaw`DELETE FROM territories WHERE id = ${enemy.id}`;
        emitTerritoryDeleted(enemy.id);

        await prisma.user.update({
          where: { id: enemy.owner_id },
          data: {
            totalAreaM2: { decrement: oldArea },
            territoriesCount: { decrement: 1 },
          },
        });

        if (enemy.clan_id) {
          await prisma.$executeRaw`
            UPDATE clans
            SET total_area_m2 = (SELECT COALESCE(SUM(area_m2), 0) FROM territories WHERE clan_id = ${enemy.clan_id}),
                territories_count = (SELECT COUNT(*) FROM territories WHERE clan_id = ${enemy.clan_id})
            WHERE id = ${enemy.clan_id}
          `;
        }
      } else {
        // Partial capture: trim enemy territory to the largest remaining piece
        const piece = pieces[0];
        const newArea: number = parseFloat(piece.piece_area);
        const areaLost = oldArea - newArea;

        await prisma.$executeRaw`
          UPDATE territories
          SET polygon    = ST_GeomFromText(${piece.piece_wkt}, 4326),
              area_m2    = ${newArea},
              perimeter_m = ${parseFloat(piece.piece_perimeter)},
              updated_at = NOW()
          WHERE id = ${enemy.id}
        `;

        await prisma.user.update({
          where: { id: enemy.owner_id },
          data: { totalAreaM2: { decrement: areaLost } },
        });

        if (enemy.clan_id) {
          await prisma.$executeRaw`
            UPDATE clans
            SET total_area_m2 = (SELECT COALESCE(SUM(area_m2), 0) FROM territories WHERE clan_id = ${enemy.clan_id}),
                territories_count = (SELECT COUNT(*) FROM territories WHERE clan_id = ${enemy.clan_id})
            WHERE id = ${enemy.clan_id}
          `;
        }

        const [updatedRow] = await prisma.$queryRaw<any[]>`
          SELECT
            t.id, t.owner_id, t.clan_id, t.area_m2, t.perimeter_m,
            t.captured_at, t.updated_at,
            ST_AsGeoJSON(t.polygon)::json AS geojson,
            u.username AS owner_username, u.color AS owner_color,
            c.color AS clan_color
          FROM territories t
          JOIN users u ON u.id = t.owner_id
          LEFT JOIN clans c ON c.id = t.clan_id
          WHERE t.id = ${enemy.id}
        `;
        emitTerritoryUpdate(rowToTerritory(updatedRow));
      }
    }

    // Check if this overlaps with user's own territories (merge)
    const ownOverlaps = await prisma.$queryRaw<any[]>`
      SELECT t.id
      FROM territories t
      WHERE t.owner_id = ${userId}
        AND ST_Intersects(
          t.polygon,
          ST_GeomFromText(${wkt}, 4326)
        )
    `;

    let merged = false;
    let territoryId: string;

    if (ownOverlaps.length > 0) {
      // Merge: union all overlapping territories + new polygon
      merged = true;
      const overlapIds = ownOverlaps.map((o) => o.id);
      const idList = overlapIds.map((id: string) => `'${id}'`).join(',');

      const [mergedGeo] = await prisma.$queryRaw<any[]>`
        SELECT
          ST_AsGeoJSON(
            ST_Union(
              ARRAY_APPEND(
                ARRAY(SELECT polygon FROM territories WHERE id = ANY(ARRAY[${overlapIds}]::uuid[])),
                ST_GeomFromText(${wkt}, 4326)
              )
            )
          )::json AS geojson,
          ST_Area(
            ST_Union(
              ARRAY_APPEND(
                ARRAY(SELECT polygon FROM territories WHERE id = ANY(ARRAY[${overlapIds}]::uuid[])),
                ST_GeomFromText(${wkt}, 4326)
              )
            )::geography
          ) AS merged_area,
          ST_Perimeter(
            ST_Union(
              ARRAY_APPEND(
                ARRAY(SELECT polygon FROM territories WHERE id = ANY(ARRAY[${overlapIds}]::uuid[])),
                ST_GeomFromText(${wkt}, 4326)
              )
            )::geography
          ) AS merged_perimeter
      `;

      // Delete old overlapping territories
      const oldAreas = await prisma.$queryRaw<any[]>`
        SELECT SUM(area_m2) AS total FROM territories WHERE id = ANY(ARRAY[${overlapIds}]::uuid[])
      `;
      const oldTotalArea: number = parseFloat(oldAreas[0]?.total ?? '0');
      const oldCount: number = overlapIds.length;

      await prisma.$executeRaw`DELETE FROM territories WHERE id = ANY(ARRAY[${overlapIds}]::uuid[])`;

      // Create merged territory
      const [created] = await prisma.$queryRaw<any[]>`
        INSERT INTO territories (id, owner_id, clan_id, polygon, area_m2, perimeter_m, captured_at, updated_at)
        VALUES (
          gen_random_uuid(),
          ${userId},
          ${user.clanId},
          ST_GeomFromText(${wkt}, 4326),
          ${parseFloat(mergedGeo.merged_area)},
          ${parseFloat(mergedGeo.merged_perimeter)},
          NOW(), NOW()
        )
        RETURNING id, area_m2, perimeter_m, captured_at, updated_at
      `;
      territoryId = created.id;

      // Update user stats
      const netAreaGain = parseFloat(mergedGeo.merged_area) - oldTotalArea;
      await prisma.user.update({
        where: { id: userId },
        data: {
          totalAreaM2: { increment: netAreaGain },
          territoriesCount: { increment: 1 - oldCount },
          capturesCount: { increment: 1 },
        },
      });
    } else {
      // New territory
      const [created] = await prisma.$queryRaw<any[]>`
        INSERT INTO territories (id, owner_id, clan_id, polygon, area_m2, perimeter_m, captured_at, updated_at)
        VALUES (
          gen_random_uuid(),
          ${userId},
          ${user.clanId},
          ST_GeomFromText(${wkt}, 4326),
          ${areaM2},
          ${perimeterM},
          NOW(), NOW()
        )
        RETURNING id, area_m2, perimeter_m, captured_at, updated_at
      `;
      territoryId = created.id;

      await prisma.user.update({
        where: { id: userId },
        data: {
          totalAreaM2: { increment: areaM2 },
          territoriesCount: { increment: 1 },
          capturesCount: { increment: 1 },
        },
      });
    }

    // Update clan stats if in clan
    if (user.clanId) {
      await prisma.$executeRaw`
        UPDATE clans
        SET total_area_m2 = (
          SELECT COALESCE(SUM(area_m2), 0) FROM territories WHERE clan_id = ${user.clanId}
        ),
        territories_count = (
          SELECT COUNT(*) FROM territories WHERE clan_id = ${user.clanId}
        )
        WHERE id = ${user.clanId}
      `;
    }

    // Fetch the created territory for response
    const [result] = await prisma.$queryRaw<any[]>`
      SELECT
        t.id, t.owner_id, t.clan_id, t.area_m2, t.perimeter_m,
        t.captured_at, t.updated_at,
        ST_AsGeoJSON(t.polygon)::json AS geojson,
        u.username AS owner_username, u.color AS owner_color,
        c.color AS clan_color
      FROM territories t
      JOIN users u ON u.id = t.owner_id
      LEFT JOIN clans c ON c.id = t.clan_id
      WHERE t.id = ${territoryId}
    `;

    const territory = rowToTerritory(result);

    // Broadcast via WebSocket
    emitTerritoryUpdate(territory);

    res.json({ success: true, territory, merged, conquered: conqueredCount, error: null });
  } catch (err) {
    console.error('[Territories] capture error:', err);
    res.status(500).json({ success: false, error: 'Internal server error', territory: null, merged: false });
  }
});

// GET /users/:id/territories  (mounted from users router, but added here for cross-reference)
export async function getUserTerritoriesHandler(req: Request, res: Response) {
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
      WHERE t.owner_id = ${req.params.id}
    `;
    res.json(rows.map(rowToTerritory));
  } catch (err) {
    console.error('[Territories] getUserTerritories error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

function rowToTerritory(row: any) {
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
    captured_at: row.captured_at instanceof Date
      ? row.captured_at.toISOString()
      : String(row.captured_at),
    updated_at: row.updated_at instanceof Date
      ? row.updated_at.toISOString()
      : String(row.updated_at),
  };
}

function haversineM(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export { router as territoriesRouter };
