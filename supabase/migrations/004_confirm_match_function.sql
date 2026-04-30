-- Squash & Go: confirm_match Postgres function
-- Atomically confirms a result_submitted match:
--   * validates the caller is the opponent (not the submitter)
--   * for ranked matches, computes new Elo ratings for both players
--     (mirrors core/domain EloCalculator: K=60 both provisional, 40 one provisional, 32 established; floor 400)
--   * writes rating_history rows
--   * marks the match as confirmed and the parent challenge as completed
--
-- Invoked from the confirm-match Edge Function with service-role privileges.
-- p_caller_id is provided explicitly by the Edge Function after JWT verification.

CREATE OR REPLACE FUNCTION confirm_match(p_match_id UUID, p_caller_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_match            matches%ROWTYPE;
    v_winner_rating    ratings%ROWTYPE;
    v_loser_rating     ratings%ROWTYPE;
    v_loser_id         UUID;
    v_k_factor         INT;
    v_winner_expected  NUMERIC;
    v_winner_new_elo   INT;
    v_loser_new_elo    INT;
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

    IF v_match.match_type = 'casual' THEN
        UPDATE matches
        SET status = 'confirmed', confirmed_at = NOW()
        WHERE id = p_match_id;

        IF v_match.challenge_id IS NOT NULL THEN
            UPDATE challenges SET status = 'completed' WHERE id = v_match.challenge_id;
        END IF;

        RETURN jsonb_build_object('success', true);
    END IF;

    -- Ranked: compute Elo
    IF v_match.winner_id IS NULL THEN
        RAISE EXCEPTION 'missing_winner' USING ERRCODE = 'P0001';
    END IF;

    v_loser_id := CASE
        WHEN v_match.winner_id = v_match.player_a_id THEN v_match.player_b_id
        ELSE v_match.player_a_id
    END;

    SELECT * INTO v_winner_rating FROM ratings WHERE player_id = v_match.winner_id FOR UPDATE;
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
    WHERE player_id = v_match.winner_id;

    UPDATE ratings SET
        elo_score = v_loser_new_elo,
        ranked_matches_played = ranked_matches_played + 1,
        is_provisional = (ranked_matches_played + 1 < 5)
    WHERE player_id = v_loser_id;

    INSERT INTO rating_history (player_id, match_id, elo_before, elo_after, k_factor)
    VALUES (v_match.winner_id, p_match_id, v_winner_rating.elo_score, v_winner_new_elo, v_k_factor);

    INSERT INTO rating_history (player_id, match_id, elo_before, elo_after, k_factor)
    VALUES (v_loser_id, p_match_id, v_loser_rating.elo_score, v_loser_new_elo, v_k_factor);

    UPDATE matches
    SET status = 'confirmed', confirmed_at = NOW()
    WHERE id = p_match_id;

    IF v_match.challenge_id IS NOT NULL THEN
        UPDATE challenges SET status = 'completed' WHERE id = v_match.challenge_id;
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'winner_new_elo', v_winner_new_elo,
        'loser_new_elo', v_loser_new_elo
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION confirm_match(UUID, UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION confirm_match(UUID, UUID) TO service_role;