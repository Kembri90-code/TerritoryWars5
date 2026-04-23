CREATE TABLE clan_join_requests (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  clan_id    UUID        NOT NULL REFERENCES clans(id)  ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL    DEFAULT NOW(),
  UNIQUE (user_id, clan_id)
);

CREATE INDEX idx_clan_join_requests_clan_id ON clan_join_requests(clan_id);
