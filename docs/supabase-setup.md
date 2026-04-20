# Supabase Setup Guide

## 1. Create Project

1. Go to [supabase.com](https://supabase.com) and create a new project
2. Choose a region close to Vancouver (US West or Canada)
3. Note your **Project URL** and **anon key** from Settings > API

## 2. Run Migration

1. Open SQL Editor in Supabase Dashboard
2. Paste the contents of `supabase/migrations/001_initial.sql`
3. Run the query

This creates all tables, indexes, RLS policies, and triggers.

## 3. Seed Court Data

1. In SQL Editor, paste `supabase/seed.sql`
2. Run to insert Metro Vancouver courts

## 4. Configure Auth

1. Go to Authentication > Providers
2. Enable **Email** provider
3. Enable **Magic Link** (should be on by default)
4. Set the redirect URL to your app's deep link scheme (e.g., `com.egr.squashgo://auth-callback`)

## 5. Configure App

Update `composeApp/build.gradle.kts` with your Supabase credentials:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
```

## 6. Verify

- Run the app and attempt a magic link login
- Check that a `players` and `ratings` row are auto-created on signup
- Verify courts are returned when fetching `/rest/v1/courts`

## Edge Functions

Deploy the `confirm-match` Edge Function:
```bash
supabase functions deploy confirm-match
```

## Database Access

- **PostgREST**: `{SUPABASE_URL}/rest/v1/{table}` — CRUD via HTTP
- **Auth (GoTrue)**: `{SUPABASE_URL}/auth/v1/` — magic link, verify, refresh
- **Edge Functions**: `{SUPABASE_URL}/functions/v1/{function-name}`
- All requests need `apikey` header + `Authorization: Bearer {access_token}` after login
