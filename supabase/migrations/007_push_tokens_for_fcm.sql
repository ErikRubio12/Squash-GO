-- Squash & Go: prepare push_tokens for FCM push notifications.
--
-- The base table already exists (migration 001) but it's not yet usable for
-- production push delivery. Three problems to fix here:
--
--   1. UNIQUE(player_id, token) lets the same FCM token be associated with
--      multiple players. That's exactly the wrong shape: when user A logs out
--      and user B logs in on the same device, FCM hands back the same token,
--      and we'd happily insert a second row — both players would receive
--      pushes for whichever event fires next. We want UNIQUE(token) so an
--      upsert (ON CONFLICT (token) DO UPDATE) re-binds the device to the
--      latest signed-in player.
--
--   2. There's no `last_seen_at` column. We need it so the send-notification
--      Edge Function can prune tokens that haven't checked in for months,
--      and so the client's periodic upsert (on each cold start) is observable.
--
--   3. There's no UPDATE policy on push_tokens, only INSERT/SELECT/DELETE.
--      Without UPDATE, the upsert path can't refresh `last_seen_at` or
--      `locale`. Without that refresh, the prune logic in (2) is meaningless.
--
-- Also adds `locale` (en/es/...) so the Edge Function has a fallback when
-- a notification absolutely needs a server-rendered string. Primary
-- localization still happens client-side from strings.xml.

-- ---------------------------------------------------------------------------
-- 1. Drop the bad uniqueness, add the right one.
--    Safe because the table is empty in all environments today.
-- ---------------------------------------------------------------------------
ALTER TABLE push_tokens
    DROP CONSTRAINT IF EXISTS push_tokens_player_id_token_key;

ALTER TABLE push_tokens
    ADD CONSTRAINT push_tokens_token_key UNIQUE (token);

-- ---------------------------------------------------------------------------
-- 2. Add the new columns.
--    last_seen_at defaults to NOW() so existing rows (if any) are sane;
--    going forward, every upsert from the client will bump it.
--    locale is nullable: clients that don't pass one don't break the upsert.
-- ---------------------------------------------------------------------------
ALTER TABLE push_tokens
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS locale       TEXT;

-- ---------------------------------------------------------------------------
-- 3. Index for the most common query: "give me all tokens for player X"
--    (called by the send-notification Edge Function for every push).
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS push_tokens_player_id_idx ON push_tokens(player_id);

-- ---------------------------------------------------------------------------
-- 4. UPDATE policy: any authenticated player can update a push_tokens row,
--    BUT the resulting row must belong to that player.
--
--    The asymmetric USING / WITH CHECK is deliberate:
--
--      USING (auth.uid() IS NOT NULL)           -- gate: who can attempt
--      WITH CHECK (auth.uid() = player_id)      -- gate: what state is allowed
--
--    Why USING isn't (auth.uid() = player_id):
--      The whole point of the upsert (INSERT ... ON CONFLICT (token) DO
--      UPDATE SET player_id = EXCLUDED.player_id, ...) is to RE-BIND a token
--      from a stale player to the current one — the case where user A logs
--      out (or doesn't) and user B logs in on the same physical device.
--      Pre-update, that row's player_id is A; post-update, it's B. A strict
--      USING (auth.uid() = player_id) would block the rebind because the
--      pre-update row doesn't belong to B yet.
--
--    Why this is safe:
--      The FCM token is a per-device secret. To target a specific row in
--      this table you'd need to know its `token`. Anyone with the token
--      already has access to the pushes (they're delivered to that device
--      regardless of DB state), so RLS isn't the right defense layer.
--      WITH CHECK still guarantees the post-update row points at the caller.
-- ---------------------------------------------------------------------------
DROP POLICY IF EXISTS "Push tokens updatable by self" ON push_tokens;
CREATE POLICY "Push tokens updatable by self" ON push_tokens
    FOR UPDATE
    USING (auth.uid() IS NOT NULL)
    WITH CHECK (auth.uid() = player_id);
