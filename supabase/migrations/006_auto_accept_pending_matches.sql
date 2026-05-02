-- Squash & Go: 72h auto-accept for pending match results
--
-- When a player submits a result, submit_result sets auto_accept_at = submitted_at + 72h.
-- If the opponent neither confirms nor disputes by then, the match is auto-confirmed.
--
-- Implementation:
--   1. Extract the Elo+writes core of confirm_match into _apply_match_confirmation(match_row),
--      so both the human path and the cron path share the exact same business logic.
--   2. Recreate confirm_match to do validation only and delegate to the helper.
--   3. Add auto_accept_pending_matches() which scans expired result_submitted matches with
--      FOR UPDATE SKIP LOCKED (concurrency-safe), and confirms each one. Per-row exception
--      handling so a single bad row doesn't kill the whole batch.
--   4. Schedule it via pg_cron every 5 minutes.
--
-- Requires the pg_cron extension to be enabled (Supabase: Database → Extensions).

CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA extensions;

-- ---------------------------------------------------------------------------
-- Helper: apply the actual confirmation. Caller is responsible for having
-- locked the match row and validated business rules (status, participants).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION _apply_match_confirmation(p_match matches)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_winner_rating    ratings%ROWTYPE;
    v_loser_rating     ratings%ROWTYPE;
    v_loser_id         UUID;
    v_k_factor         INT;
    v_winner_expected  NUMERIC;
    v_winner_new_elo   INT;
    v_loser_new_elo    INT;
BEGIN
    IF p_match.match_type = 'casual' THEN
        UPDATE matches
        SET status = 'confirmed', confirmed_at = NOW()
        WHERE id = p_match.id;

        IF p_match.challenge_id IS NOT NULL THEN
            UPDATE challenges SET status = 'completed' WHERE id = p_match.challenge_id;
        END IF;

        RETURN jsonb_build_object('success', true);
    END IF;

    IF p_match.winner_id IS NULL THEN
        RAISE EXCEPTION 'missing_winner' USING ERRCODE = 'P0001';
    END IF;

    v_loser_id := CASE
        WHEN p_match.winner_id = p_match.player_a_id THEN p_match.player_b_id
        ELSE p_match.player_a_id
    END;

    SELECT * INTO v_winner_rating FROM ratings WHERE player_id = p_match.winner_id FOR UPDATE;
    SELECT * INTO v_loser_rating  FROM ratings WHERE player_id = v_loser_id        FOR UPDATE;

    v_k_factor := CASE
        WHEN v_winner_rating.ranked_matches_played < 5 AND v_loser_rating.ranked_matches_played < 5 THEN 60
        WHEN v_winner_rating.ranked_matches_played < 5 OR  v_loser_rating.ranked_matches_played < 5 THEN 40
        ELSE 32
    END;

    v_winner_expected := 1.0 / (1.0 + power(10.0, (v_loser_rating.elo_score - v_winner_rating.elo_score)::NUMERIC / 400.0));

    v_winner_new_elo := GREATEST(400, ROUND(v_winner_rating.elo_score + v_k_factor * (1.0 - v_winner_expected))::INT);
    v_loser_new_elo  := GREATEST(400, ROUND(v_loser_rating.elo_score  + v_k_factor * (0.0 - (1.0 - v_winner_expected)))::INT);

    UPDATE ratings SET
        elo_score = v_winner_new_elo,
        ranked_matches_played = ranked_matches_played + 1,
        is_provisional = (ranked_matches_played + 1 < 5)
    WHERE player_id = p_match.winner_id;

    UPDATE ratings SET
        elo_score = v_loser_new_elo,
        ranked_matches_played = ranked_matches_played + 1,
        is_provisional = (ranked_matches_played + 1 < 5)
    WHERE player_id = v_loser_id;

    INSERT INTO rating_history (player_id, match_id, elo_before, elo_after, k_factor)
    VALUES (p_match.winner_id, p_match.id, v_winner_rating.elo_score, v_winner_new_elo, v_k_factor);

    INSERT INTO rating_history (player_id, match_id, elo_before, elo_after, k_factor)
    VALUES (v_loser_id, p_match.id, v_loser_rating.elo_score, v_loser_new_elo, v_k_factor);

    UPDATE matches
    SET status = 'confirmed', confirmed_at = NOW()
    WHERE id = p_match.id;

    IF p_match.challenge_id IS NOT NULL THEN
        UPDATE challenges SET status = 'completed' WHERE id = p_match.challenge_id;
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'winner_new_elo', v_winner_new_elo,
        'loser_new_elo', v_loser_new_elo
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION _apply_match_confirmation(matches) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION _apply_match_confirmation(matches) TO service_role;

-- ---------------------------------------------------------------------------
-- Recreate confirm_match: validate, lock, delegate.
-- Behavior is identical to the previous version (migration 004).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION confirm_match(p_match_id UUID, p_caller_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_match matches%ROWTYPE;
BEGIN
    SELECT * INTO v_match FROM matches WHERE id = p_match_id FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'match_not_found' USING ERRCODE = 'P0002';
    END IF;

    IF v_match.status <> 'result_submitted' THEN
        RAISE EXCEPTION 'invalid_status' USING ERRCODE = 'P0001';
    END IF;

    IF p_caller_id NOT IN (v_match.player_a_id, v_match.player_b_id) THEN
        RAISE EXCEPTION 'not_a_participant' USING ERRCODE = 'P0001';
    END IF;

    IF p_caller_id = v_match.submitted_by_id THEN
        RAISE EXCEPTION 'cannot_confirm_own_submission' USING ERRCODE = 'P0001';
    END IF;

    RETURN _apply_match_confirmation(v_match);
END;
$$;

-- ---------------------------------------------------------------------------
-- Cron-driven auto-accept. Returns the number of matches processed.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION auto_accept_pending_matches()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_match     matches%ROWTYPE;
    v_processed INT := 0;
BEGIN
    FOR v_match IN
        SELECT *
        FROM matches
        WHERE status = 'result_submitted'
          AND auto_accept_at IS NOT NULL
          AND auto_accept_at < NOW()
          AND winner_id IS NOT NULL
        ORDER BY auto_accept_at
        FOR UPDATE SKIP LOCKED
    LOOP
        BEGIN
            PERFORM _apply_match_confirmation(v_match);
            v_processed := v_processed + 1;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'auto_accept_pending_matches: skipping match % due to: %',
                v_match.id, SQLERRM;
        END;
    END LOOP;
    RETURN v_processed;
END;
$$;

REVOKE EXECUTE ON FUNCTION auto_accept_pending_matches() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION auto_accept_pending_matches() TO postgres, service_role;

-- ---------------------------------------------------------------------------
-- Schedule the cron job every 5 minutes.
-- Idempotent: unschedule any prior job with the same name first.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    PERFORM cron.unschedule('auto-accept-pending-matches')
    WHERE EXISTS (
        SELECT 1 FROM cron.job WHERE jobname = 'auto-accept-pending-matches'
    );
EXCEPTION WHEN OTHERS THEN
    -- swallow — cron schema may not exist on a fresh DB; we'll create the job below
    NULL;
END $$;

SELECT cron.schedule(
    'auto-accept-pending-matches',
    '*/5 * * * *',
    $cron$ SELECT auto_accept_pending_matches(); $cron$
);
