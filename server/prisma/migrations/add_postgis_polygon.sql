-- Run this AFTER prisma migrate deploy or prisma db push
-- Adds the PostGIS polygon geometry column to territories table

-- Enable PostGIS if not already enabled
CREATE EXTENSION IF NOT EXISTS postgis;

-- Add polygon column (Prisma can't manage PostGIS types natively)
ALTER TABLE territories
  ADD COLUMN IF NOT EXISTS polygon geometry(Polygon, 4326);

-- Spatial index for fast bounding box queries
CREATE INDEX IF NOT EXISTS territories_polygon_gist
  ON territories USING GIST (polygon);

-- Index for owner lookups
CREATE INDEX IF NOT EXISTS territories_owner_id_idx
  ON territories (owner_id);

CREATE INDEX IF NOT EXISTS territories_clan_id_idx
  ON territories (clan_id);

-- Index for leaderboard queries
CREATE INDEX IF NOT EXISTS users_total_area_idx
  ON users (total_area_m2 DESC);

CREATE INDEX IF NOT EXISTS users_captures_idx
  ON users (captures_count DESC);

CREATE INDEX IF NOT EXISTS users_distance_idx
  ON users (distance_walked_m DESC);

CREATE INDEX IF NOT EXISTS clans_total_area_idx
  ON clans (total_area_m2 DESC);
