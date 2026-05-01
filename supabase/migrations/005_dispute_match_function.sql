-- Squash & Go: dispute_match Postgres function
-- Atomically disputes a result_submitted match:
--   * validates the caller is the opponent (not the submitter)
--   * inserts a disputes row with the provided reason
--   * transitions the match to 'disputed'
--
-- The challenge row is deliberately left untouched — the dispute is open until
-- an admin resolves it; only at resolution does the challenge close.
--
-- Invoked from the dispute-match Edge Function with service-role privileges.
-- p_caller_id is provided explicitly by the Edge Function after JWT verification.

CREATE OR REPLACE FUNCTION dispute_match(
    p_match_id UUID,
    p_caller_id UUID,
    p_reason TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_match    matches%ROWTYPE;
    v_dispute  disputes%ROWTYPE;
BEGIN
    IF p_reason IS NULL OR length(btrim(p_reason)) = 0 THEN
        RAISE EXCEPTION 'missing_reason' USING ERRCODE = 'P0001';
    END IF;

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
        RAISE EXCEPTION 'cannot_dispute_own_submission' USING ERRCODE = 'P0001';
    END IF;

    INSERT INTO disputes (match_id, raised_by_id, reason)
    VALUES (p_match_id, p_caller_id, btrim(p_reason))
    RETURNING * INTO v_dispute;

    UPDATE matches
    SET status = 'disputed'
    WHERE id = p_match_id;

    RETURN jsonb_build_object(
        'success', true,
        'dispute_id', v_dispute.id
    );
END;
$$;

REVOKE EXECUTE ON FUNCTION dispute_match(UUID, UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dispute_match(UUID, UUID, TEXT) TO service_role;
