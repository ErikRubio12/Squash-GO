-- Fix: "Database error saving new user" (HTTP 500) on signup.
--
-- Root cause: the supabase_auth_admin role (the one that performs the
-- INSERT into auth.users during signup) lacked EXECUTE on the trigger
-- function, and the function had no fixed search_path. Both are well-
-- known footguns for SECURITY DEFINER triggers on auth.users in
-- recent Supabase releases.

-- 1. Recreate the function with a fixed search_path so it always finds
--    public.players / public.ratings regardless of the caller's
--    session search_path.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.players (id, display_name)
    VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'display_name', ''));

    INSERT INTO public.ratings (player_id)
    VALUES (NEW.id);

    RETURN NEW;
END;
$$;

-- 2. Make sure the auth admin role can reach the schema and execute
--    the trigger function. Without these grants the trigger fires but
--    the invoking role (supabase_auth_admin) gets a permission error,
--    which Supabase surfaces as a 500 "Database error saving new user".
GRANT USAGE ON SCHEMA public TO supabase_auth_admin;
GRANT EXECUTE ON FUNCTION public.handle_new_user() TO supabase_auth_admin;
