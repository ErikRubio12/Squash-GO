-- Squash & Go: Initial Database Schema
-- Run this migration in Supabase SQL Editor or via CLI: supabase db push

-- ============================================
-- TABLES
-- ============================================

-- Players (extends auth.users)
CREATE TABLE players (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT,
    availability_note TEXT,
    is_inactive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Courts
CREATE TABLE courts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    number_of_courts INT,
    phone_number TEXT,
    website TEXT,
    is_verified BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Player-Court association (many-to-many)
CREATE TABLE player_courts (
    player_id UUID REFERENCES players(id) ON DELETE CASCADE,
    court_id UUID REFERENCES courts(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (player_id, court_id)
);

-- Ratings
CREATE TABLE ratings (
    player_id UUID PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    elo_score INT DEFAULT 1200,
    ranked_matches_played INT DEFAULT 0,
    is_provisional BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Challenges
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenger_id UUID NOT NULL REFERENCES players(id),
    challenged_id UUID NOT NULL REFERENCES players(id),
    court_id UUID REFERENCES courts(id),
    match_type TEXT NOT NULL CHECK (match_type IN ('casual', 'ranked')),
    message TEXT,
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'declined', 'cancelled', 'expired', 'completed')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '7 days')
);

-- Matches
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID REFERENCES challenges(id),
    player_a_id UUID NOT NULL REFERENCES players(id),
    player_b_id UUID NOT NULL REFERENCES players(id),
    court_id UUID REFERENCES courts(id),
    match_type TEXT NOT NULL CHECK (match_type IN ('casual', 'ranked')),
    status TEXT NOT NULL DEFAULT 'in_progress'
        CHECK (status IN ('in_progress', 'result_submitted', 'confirmed', 'disputed', 'cancelled')),
    submitted_by_id UUID REFERENCES players(id),
    score JSONB,
    winner_id UUID REFERENCES players(id),
    played_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    auto_accept_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Disputes (MVP-minimal)
CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id),
    raised_by_id UUID NOT NULL REFERENCES players(id),
    reason TEXT NOT NULL,
    admin_notes TEXT,
    resolution TEXT CHECK (resolution IN ('score_accepted', 'score_corrected', 'match_cancelled')),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Rating history (audit trail)
CREATE TABLE rating_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players(id),
    match_id UUID NOT NULL REFERENCES matches(id),
    elo_before INT NOT NULL,
    elo_after INT NOT NULL,
    k_factor INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Push notification tokens
CREATE TABLE push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players(id),
    token TEXT NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('android', 'ios')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (player_id, token)
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_player_courts_court ON player_courts(court_id);
CREATE INDEX idx_challenges_challenged ON challenges(challenged_id, status);
CREATE INDEX idx_challenges_challenger ON challenges(challenger_id, status);
CREATE INDEX idx_matches_player_a ON matches(player_a_id);
CREATE INDEX idx_matches_player_b ON matches(player_b_id);
CREATE INDEX idx_matches_status ON matches(status) WHERE status IN ('result_submitted', 'in_progress');
CREATE INDEX idx_matches_auto_accept ON matches(auto_accept_at) WHERE status = 'result_submitted';
CREATE INDEX idx_rating_history_player ON rating_history(player_id);

-- ============================================
-- ROW LEVEL SECURITY
-- ============================================

-- Players
ALTER TABLE players ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Players readable by authenticated" ON players
    FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Players updatable by self" ON players
    FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Players insertable by self" ON players
    FOR INSERT WITH CHECK (auth.uid() = id);

-- Courts (read-only for users, admin seeds via service role)
ALTER TABLE courts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Courts readable by authenticated" ON courts
    FOR SELECT USING (auth.role() = 'authenticated');

-- Player_courts
ALTER TABLE player_courts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Player_courts readable by authenticated" ON player_courts
    FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Player_courts insertable by self" ON player_courts
    FOR INSERT WITH CHECK (auth.uid() = player_id);
CREATE POLICY "Player_courts deletable by self" ON player_courts
    FOR DELETE USING (auth.uid() = player_id);

-- Ratings (readable by all, writable only via Edge Functions using service_role)
ALTER TABLE ratings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Ratings readable by authenticated" ON ratings
    FOR SELECT USING (auth.role() = 'authenticated');

-- Challenges
ALTER TABLE challenges ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Challenges readable by participants" ON challenges
    FOR SELECT USING (auth.uid() IN (challenger_id, challenged_id));
CREATE POLICY "Challenges insertable by challenger" ON challenges
    FOR INSERT WITH CHECK (auth.uid() = challenger_id);
CREATE POLICY "Challenges updatable by participants" ON challenges
    FOR UPDATE USING (auth.uid() IN (challenger_id, challenged_id));

-- Matches
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Matches readable by participants" ON matches
    FOR SELECT USING (auth.uid() IN (player_a_id, player_b_id));
CREATE POLICY "Matches insertable by participants" ON matches
    FOR INSERT WITH CHECK (auth.uid() IN (player_a_id, player_b_id));
CREATE POLICY "Matches updatable by participants" ON matches
    FOR UPDATE USING (auth.uid() IN (player_a_id, player_b_id));

-- Disputes
ALTER TABLE disputes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Disputes readable by match participants" ON disputes
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM matches
            WHERE matches.id = disputes.match_id
            AND auth.uid() IN (matches.player_a_id, matches.player_b_id)
        )
    );
CREATE POLICY "Disputes insertable by match participants" ON disputes
    FOR INSERT WITH CHECK (auth.uid() = raised_by_id);

-- Rating history
ALTER TABLE rating_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Rating history readable by self" ON rating_history
    FOR SELECT USING (auth.uid() = player_id);

-- Push tokens
ALTER TABLE push_tokens ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Push tokens readable by self" ON push_tokens
    FOR SELECT USING (auth.uid() = player_id);
CREATE POLICY "Push tokens insertable by self" ON push_tokens
    FOR INSERT WITH CHECK (auth.uid() = player_id);
CREATE POLICY "Push tokens deletable by self" ON push_tokens
    FOR DELETE USING (auth.uid() = player_id);

-- ============================================
-- TRIGGERS
-- ============================================

-- Auto-create player and rating on new auth user signup
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO players (id, display_name)
    VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'display_name', ''));

    INSERT INTO ratings (player_id)
    VALUES (NEW.id);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_players_updated_at
    BEFORE UPDATE ON players
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_challenges_updated_at
    BEFORE UPDATE ON challenges
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_matches_updated_at
    BEFORE UPDATE ON matches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_ratings_updated_at
    BEFORE UPDATE ON ratings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
