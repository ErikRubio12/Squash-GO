-- Squash & Go: server-driven brand palettes.
--
-- Stores the brand color tokens that drive the design system at runtime. The mobile
-- client fetches the currently active palette on demand (e.g. when the user pulls to
-- refresh or when push tells the app to re-sync). The on-device `ThemeResolver` applies
-- the palette to Material 3 ColorScheme; widgets recompose automatically.
--
-- Why a single jsonb column instead of 24 typed columns:
--   * Adding new tokens (typography, shapes, motion) later does not require a migration
--     — only an app release that knows how to read the new keys.
--   * Easier to copy/paste a palette in the Supabase dashboard for the demo.
--   * Cheaper to validate end-to-end: the DTO mirrors the JSON shape 1:1.
--
-- Constraint: at most ONE row may have is_active = true at a time. The partial unique
-- index enforces this without preventing many inactive rows.

CREATE TABLE IF NOT EXISTS public.brand_palettes (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       text        NOT NULL,
    is_active  boolean     NOT NULL DEFAULT false,
    palette    jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Only one active palette at a time.
CREATE UNIQUE INDEX IF NOT EXISTS brand_palettes_one_active
    ON public.brand_palettes(is_active)
    WHERE is_active = true;

-- RLS: anonymous read access for the active palette only. Writes require service role
-- (managed via Supabase dashboard or Edge Functions). No client should mutate brand
-- tokens directly.
ALTER TABLE public.brand_palettes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read of active brand palette" ON public.brand_palettes;
CREATE POLICY "Public read of active brand palette"
    ON public.brand_palettes
    FOR SELECT
    USING (is_active = true);

-- ---------------------------------------------------------------------------
-- Seed data: three sample palettes. Vancouver is the active one on first deploy,
-- so the app's "Fetch latest from server" demo button works immediately.
--
-- To toggle which palette is active from the Supabase dashboard, run:
--   UPDATE brand_palettes SET is_active = false;
--   UPDATE brand_palettes SET is_active = true WHERE name = 'Toronto Squash Club';
-- ---------------------------------------------------------------------------

INSERT INTO public.brand_palettes (name, is_active, palette) VALUES
(
    'SquashGo Vancouver',
    true,
    '{
        "name": "SquashGo Vancouver",
        "light": {
            "primary": "#1B5E20", "on_primary": "#FFFFFF",
            "primary_container": "#A5D6A7", "on_primary_container": "#0A2E0F",
            "secondary": "#4CAF50", "on_secondary": "#FFFFFF",
            "background": "#FAFAFA", "on_background": "#1A1A1A",
            "surface": "#FFFFFF", "on_surface": "#1A1A1A",
            "error": "#B00020", "on_error": "#FFFFFF"
        },
        "dark": {
            "primary": "#81C784", "on_primary": "#0A2E0F",
            "primary_container": "#1B5E20", "on_primary_container": "#A5D6A7",
            "secondary": "#66BB6A", "on_secondary": "#0A2E0F",
            "background": "#121212", "on_background": "#E0E0E0",
            "surface": "#1E1E1E", "on_surface": "#E0E0E0",
            "error": "#CF6679", "on_error": "#000000"
        }
    }'::jsonb
),
(
    'Toronto Squash Club',
    false,
    '{
        "name": "Toronto Squash Club",
        "light": {
            "primary": "#0D47A1", "on_primary": "#FFFFFF",
            "primary_container": "#BBDEFB", "on_primary_container": "#002171",
            "secondary": "#1976D2", "on_secondary": "#FFFFFF",
            "background": "#F5F7FA", "on_background": "#14202E",
            "surface": "#FFFFFF", "on_surface": "#14202E",
            "error": "#D32F2F", "on_error": "#FFFFFF"
        },
        "dark": {
            "primary": "#82B1FF", "on_primary": "#002171",
            "primary_container": "#0D47A1", "on_primary_container": "#BBDEFB",
            "secondary": "#64B5F6", "on_secondary": "#002171",
            "background": "#0F1620", "on_background": "#E0E6EE",
            "surface": "#18202C", "on_surface": "#E0E6EE",
            "error": "#FF8A80", "on_error": "#000000"
        }
    }'::jsonb
),
(
    'Bronze Tournament',
    false,
    '{
        "name": "Bronze Tournament",
        "light": {
            "primary": "#8D5524", "on_primary": "#FFFFFF",
            "primary_container": "#FFD8B4", "on_primary_container": "#3E1F00",
            "secondary": "#CD853F", "on_secondary": "#FFFFFF",
            "background": "#FFF8F0", "on_background": "#2A1A0A",
            "surface": "#FFFCF7", "on_surface": "#2A1A0A",
            "error": "#B00020", "on_error": "#FFFFFF"
        },
        "dark": {
            "primary": "#E6B07A", "on_primary": "#3E1F00",
            "primary_container": "#8D5524", "on_primary_container": "#FFD8B4",
            "secondary": "#D2A06A", "on_secondary": "#3E1F00",
            "background": "#1A120A", "on_background": "#E8DCC8",
            "surface": "#231811", "on_surface": "#E8DCC8",
            "error": "#CF6679", "on_error": "#000000"
        }
    }'::jsonb
);
