-- Squash & Go: make brand palette activation atomic.
--
-- The partial unique index in migration 008 enforces "at most one active palette" but
-- creates an awkward UX in the dashboard: you cannot just toggle `is_active = true` on a
-- new row, because the existing active row still holds the slot. You have to deactivate
-- the previous row first, in the same transaction.
--
-- This trigger does that deactivation automatically, BEFORE INSERT or UPDATE. When a row
-- is about to become active, every *other* active row is flipped to inactive first. The
-- new row then proceeds and the partial unique index sees exactly one active row.
--
-- Why BEFORE (not AFTER): BEFORE triggers fire before constraints are checked. By
-- deactivating other rows in the BEFORE hook, the partial unique index has a clean state
-- when it evaluates the new/updated row. An AFTER trigger would race against the index.
--
-- Why `WHEN (NEW.is_active = true)`: skip the work when a row is being inserted/updated
-- inactive — no need to scan the table for nothing. Avoids recursion: the inner UPDATE
-- below sets is_active = false, which does not match the WHEN clause and so does not
-- re-fire this trigger.

CREATE OR REPLACE FUNCTION public.deactivate_other_brand_palettes()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.brand_palettes
    SET is_active = false
    WHERE id <> NEW.id AND is_active = true;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS brand_palettes_only_one_active ON public.brand_palettes;
CREATE TRIGGER brand_palettes_only_one_active
    BEFORE INSERT OR UPDATE ON public.brand_palettes
    FOR EACH ROW
    WHEN (NEW.is_active = true)
    EXECUTE FUNCTION public.deactivate_other_brand_palettes();
