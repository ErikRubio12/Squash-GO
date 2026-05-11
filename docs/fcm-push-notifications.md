# FCM Push Notifications — MVP Plan

## Goal

Notify players in real time about events that drive the match-play loop, so the app's north star (completed matches) doesn't depend on users polling the app.

**Targets**: Android only for MVP. iOS uses APNs-via-FCM later (Phase 4); the server side is shared.

## Scope (MVP)

Events that earn a push:

| Event | Recipient | Trigger surface | Deep link |
|---|---|---|---|
| Challenge created | Challenged player | Insert on `challenges` (status=`pending`) | `squashgo://challenge-detail/<id>` |
| Challenge accepted | Challenger | Update `challenges.status` → `accepted` | `squashgo://challenge-detail/<id>` |
| Challenge declined | Challenger | Update `challenges.status` → `declined` | `squashgo://challenge-detail/<id>` |
| Challenge cancelled | Other party | Update `challenges.status` → `cancelled` | `squashgo://challenges` |
| Match result submitted | Opponent | `submit_result` RPC (status=`result_submitted`) | `squashgo://match-confirm/<id>` |
| Match confirmed | Submitter | `_apply_match_confirmation` (manual + auto-accept) | `squashgo://match-detail/<id>` |
| Match disputed | Submitter | `dispute_match` RPC | `squashgo://match-detail/<id>` |

Out of scope for this iteration: marketing pushes, in-app notification center, badge counts, silent data sync.

## Architecture

```
┌──────────────────────┐        ┌────────────────────────────────────┐
│   Postgres (Supabase)│        │      Edge Function (Deno)          │
│  - migration 007:    │        │  send-notification                 │
│    push_tokens     │        │  - service-role only               │
│  - triggers:         │ pg_net │  - looks up push_tokens          │
│    challenges_notify │───────▶│  - mints FCM v1 OAuth token        │
│  - existing RPCs:    │        │  - POST to FCM HTTP v1             │
│    confirm/dispute   │        │  - prunes invalid tokens           │
│    explicitly call   │        └────────────────┬───────────────────┘
│    send-notification │                         │
└──────────────────────┘                         │
                                                 ▼
                                ┌──────────────────────────────────┐
                                │   FCM HTTP v1                    │
                                │   /v1/projects/<id>/messages:send│
                                └──────────────┬───────────────────┘
                                               │ data-only payload
                                               ▼
                          ┌──────────────────────────────────────────┐
                          │  Android :app                            │
                          │  SquashGoMessagingService                │
                          │  - onNewToken → upsert via REST          │
                          │  - onMessageReceived → render i18n notif │
                          │  - tap → deep link into NavHost          │
                          └──────────────────────────────────────────┘
```

### Why data-only payloads (not `notification` + `data`)

Server-side localization is awkward and we already have `strings.xml` per-locale on Android (and per-locale on iOS later). FCM `notification` payloads are auto-displayed by the system in the **server-provided language** when the app is backgrounded — that bypasses our localization.

Solution: send **data-only** messages, run a foreground service handler that builds the `NotificationCompat.Builder` from local string resources, then we have full control over title/body, channel, deep link, and grouping. Trade-off: data-only messages have lower priority unless we set `priority=high`; we'll do that for the time-sensitive events (result submitted, confirmed).

## 1. Schema — `supabase/migrations/007_push_tokens_for_fcm.sql`

The table `push_tokens` already exists from migration 001 with `(id, player_id, token, platform, created_at, UNIQUE(player_id, token))`, RLS enabled, and SELECT/INSERT/DELETE policies. Migration 007 evolves it instead of creating a new table:

- Drop `UNIQUE(player_id, token)` and replace with `UNIQUE(token)`. Without this, the same FCM token can map to two players — when user A logs out and B logs in on the same phone, FCM hands back the same token and the second insert succeeds, so pushes go to the wrong account. With `UNIQUE(token)`, the upsert (`ON CONFLICT (token) DO UPDATE SET player_id = EXCLUDED.player_id, last_seen_at = NOW()`) re-binds the device to the new player.
- Add `last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` so the Edge Function can prune stale tokens by age, and so each cold-start upsert is observable.
- Add `locale TEXT` (nullable) as a server-side fallback hint. Primary localization still happens client-side from `strings.xml`.
- Add `CREATE INDEX push_tokens_player_id_idx` — the hot query is "all tokens for player X", run on every push.
- Add an UPDATE policy ("Push tokens updatable by self", `auth.uid() = player_id` on USING and WITH CHECK). Required so the client upsert can refresh `last_seen_at` and `locale` without escalating to service-role.

Field naming follows the existing schema convention (`player_id`, not `user_id`, since `players.id` is itself the FK to `auth.users(id)`).

## 2. Edge Function — `supabase/functions/send-notification/index.ts`

**Internal use only**: invoked by SQL triggers (via `pg_net.http_post`) and by the existing `confirm-match` / `dispute-match` functions. Authenticates via a shared secret in the `Authorization` header (NOT a user JWT), set as `INTERNAL_FN_SHARED_SECRET` env var.

```ts
interface SendNotificationRequest {
  recipient_user_id: string;
  type: NotificationType;          // enum mirrored on Android
  data: Record<string, string>;    // arbitrary, e.g. { match_id, deep_link }
  title_key?: string;              // optional fallback if localization not possible
  body_key?: string;
}
```

Responsibilities:
1. Verify shared secret (constant-time compare).
2. Query `push_tokens` for `recipient_user_id` (service role).
3. Mint a Google OAuth2 access token from the FCM service-account JSON (cached for 50 min).
4. POST one FCM message per token to `https://fcm.googleapis.com/v1/projects/<project>/messages:send`.
5. On `UNREGISTERED` / `INVALID_ARGUMENT` responses → `DELETE` the offending row.
6. Best-effort: errors per-token are logged, not propagated to the caller.

**FCM message shape**:

```json
{
  "message": {
    "token": "<token>",
    "data": {
      "type": "MATCH_RESULT_SUBMITTED",
      "match_id": "uuid",
      "deep_link": "squashgo://match-confirm/<id>",
      "submitter_name": "Alice"
    },
    "android": {
      "priority": "HIGH"
    }
  }
}
```

**Env vars to add to Supabase project**:
- `FCM_PROJECT_ID`
- `FCM_SERVICE_ACCOUNT_JSON` (entire JSON string)
- `INTERNAL_FN_SHARED_SECRET` (random 64+ chars)

## 3. Trigger / hook layer — `supabase/migrations/008_notification_triggers.sql`

Two strategies, used together:

### a. Postgres triggers via `pg_net` (for events that don't already pass through a function)

```sql
CREATE EXTENSION IF NOT EXISTS pg_net WITH SCHEMA extensions;

CREATE OR REPLACE FUNCTION notify_on_challenge_event()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_recipient UUID;
    v_type      TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_recipient := NEW.challenged_id;
        v_type      := 'CHALLENGE_CREATED';
    ELSIF TG_OP = 'UPDATE' AND OLD.status <> NEW.status THEN
        IF NEW.status = 'accepted' THEN
            v_recipient := NEW.challenger_id;
            v_type      := 'CHALLENGE_ACCEPTED';
        ELSIF NEW.status = 'declined' THEN
            v_recipient := NEW.challenger_id;
            v_type      := 'CHALLENGE_DECLINED';
        ELSIF NEW.status = 'cancelled' THEN
            v_recipient := CASE WHEN NEW.cancelled_by = NEW.challenger_id
                                THEN NEW.challenged_id
                                ELSE NEW.challenger_id END;
            v_type      := 'CHALLENGE_CANCELLED';
        ELSE
            RETURN NEW;
        END IF;
    ELSE
        RETURN NEW;
    END IF;

    PERFORM net.http_post(
        url     := current_setting('app.send_notification_url'),
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'Authorization', current_setting('app.internal_fn_secret')
        ),
        body    := jsonb_build_object(
            'recipient_user_id', v_recipient,
            'type',              v_type,
            'data',              jsonb_build_object('challenge_id', NEW.id)
        )
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER challenges_notify_after_insert
    AFTER INSERT ON challenges
    FOR EACH ROW EXECUTE FUNCTION notify_on_challenge_event();

CREATE TRIGGER challenges_notify_after_update
    AFTER UPDATE ON challenges
    FOR EACH ROW EXECUTE FUNCTION notify_on_challenge_event();
```

Settings `app.send_notification_url` and `app.internal_fn_secret` are seeded once from the Supabase dashboard (Database → Settings → Custom Configuration), so URLs aren't hardcoded into migrations.

### b. Direct invocation from existing Edge Functions (events that already go through service-role code)

Extend the existing functions to fire-and-forget a fetch to `send-notification` after the RPC returns successfully:

- `confirm-match/index.ts` → notify submitter (`MATCH_CONFIRMED`).
- `dispute-match/index.ts` → notify submitter (`MATCH_DISPUTED`).
- `auto_accept_pending_matches()` (already in migration 006) → for each row it confirms, call `send-notification` for both players (`MATCH_AUTO_CONFIRMED`).
- The match submission path: today this is a direct PostgREST insert, not a function. Either (a) move it behind `submit_result` RPC + edge wrapper that also notifies, or (b) add a Postgres trigger on `matches` AFTER UPDATE WHEN status changed to `result_submitted`. **Recommended: option (b)**, mirrors the challenges pattern and keeps client code unchanged.

## 4. Android wiring

### 4.1 Gradle / build setup
- Add `google-services` plugin classpath at root (`build.gradle.kts`).
- Apply `com.google.gms.google-services` in `:app/build.gradle.kts`.
- Drop `google-services.json` in `:app/` (and `:app/src/<flavor>/` if we add per-client flavors later — see `white-label.md`).
- Firebase BOM + `firebase-messaging-ktx` in `:app/build.gradle.kts`.

### 4.2 New module — `:core:notifications` (Android)
Owns:
- `NotificationType` enum (mirror of server enum).
- `NotificationChannelRegistrar` invoked from `SquashGoApplication.onCreate`.
- `NotificationRenderer.show(context, type, data)` — looks up title/body via `strings.xml`, builds intent with deep link, posts via `NotificationManagerCompat`.
- `PushTokenRegistrar` — coroutine-friendly facade that:
  1. Reads current FCM token (`FirebaseMessaging.getInstance().token.await()`).
  2. Calls `PushTokenRepository.upsert(token, locale, "android")`.

Why a separate module: `SquashGoMessagingService` lives in `:app` (manifest entry), but the rendering / repo wiring is reusable from `:app` and from any background work (token refresh from a worker, etc.).

### 4.3 `:data:network`
- New `PushTokenApi.kt` — PostgREST upsert against `push_tokens`.
- New `PushTokenRepository` interface in `:core:domain` + `SupabasePushTokenRepository` impl.
- Wire in `:app/di/RepositoryModule`.

### 4.4 `:app`
- `SquashGoMessagingService : FirebaseMessagingService` (registered in manifest):
  - `onNewToken(token)` → enqueue a `OneTimeWorkRequest` that calls `PushTokenRegistrar.upsert` (workers retry on failure; can't run coroutines on the service forever).
  - `onMessageReceived(remoteMessage)` → `NotificationRenderer.show(...)` if app is backgrounded; if foregrounded, drop a `Snackbar`/in-app event via a `NotificationEventBus` (StateFlow in `:core:notifications`).
- `MainActivity` — extend the existing deep-link handler to recognize the new `squashgo://challenge-detail/<id>`, `squashgo://match-confirm/<id>`, `squashgo://match-detail/<id>` schemes and navigate to the right route.
- `AppBootstrapViewModel` — once `BootstrapState.Ready`, kick off `PushTokenRegistrar.upsert` (only after we have a valid session, otherwise the upsert RLS check fails). Also call `sessionManager.clearSession()` flow (in `logout()`) → DELETE the current device token before clearing state.
- Manifest:
  - `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` (Android 13+).
  - `<service>` entry for `SquashGoMessagingService`.
- Runtime permission prompt — request `POST_NOTIFICATIONS` after first successful login (educational rationale screen first; rejection is non-fatal, just record it).

### 4.5 i18n & strings
Add to a new `:core:notifications/src/main/res/values{,-es}/strings.xml`:
- `notif_challenge_created_title`, `notif_challenge_created_body` (`%1$s` = challenger name).
- `notif_challenge_accepted_*`, `notif_challenge_declined_*`, `notif_challenge_cancelled_*`.
- `notif_match_result_submitted_*` (`%1$s` = opponent name).
- `notif_match_confirmed_*` (with `%1$d` new Elo).
- `notif_match_disputed_*`.
- `notif_match_auto_confirmed_*`.
- `notif_channel_match_play_name`, `notif_channel_match_play_desc`.

One channel for MVP: `match_play` (high importance). Split later if we add marketing.

## 5. End-to-end flow examples

**Match result submitted** (most time-sensitive path):
1. Player A taps "Submit result" → `MatchResultViewModel` calls REST `PATCH matches`.
2. Postgres trigger on `matches` AFTER UPDATE fires `notify_on_match_event`.
3. Trigger calls `send-notification` via `pg_net.http_post`.
4. Edge function looks up B's device tokens, hits FCM HTTP v1 with `priority=HIGH`.
5. B's phone receives data message, `SquashGoMessagingService.onMessageReceived` builds notification with localized title/body and `squashgo://match-confirm/<id>`.
6. B taps notification → `MainActivity` deep-link handler navigates to `MatchConfirmRoute(matchId)`.

**Token rotation** (FCM rotates tokens periodically):
1. `onNewToken(newToken)` fires → enqueue `RegisterPushTokenWorker`.
2. Worker calls `PushTokenRepository.upsert(newToken, currentLocale, "android")`.
3. PostgREST `INSERT ... ON CONFLICT (token) DO UPDATE` re-binds token to current user; old user's row remains until that user signs in on this device again (then upsert overwrites).

## Local development setup

One-time prerequisites for running Supabase Edge Functions locally:

```bash
# 1. Supabase CLI
brew install supabase/tap/supabase
supabase login
supabase link --project-ref <project-ref-from-dashboard-url>

# 2. Container runtime (Edge Functions run in a Deno container locally)
#    Either Docker Desktop OR OrbStack (lighter, drop-in compatible)
brew install --cask docker        # or: brew install --cask orbstack
open -a Docker                    # or: open -a OrbStack
```

Per-session:

```bash
supabase start                                    # boots local Postgres + auth + edge runtime
supabase functions serve send-notification \
    --env-file supabase/functions/.env \
    --no-verify-jwt                               # function uses shared secret, not JWT
```

The local `.env` (gitignored) holds the five env vars from `.env.example`:
`SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `FCM_PROJECT_ID`, `FCM_SERVICE_ACCOUNT_JSON`, `INTERNAL_FN_SHARED_SECRET`.

`FCM_SERVICE_ACCOUNT_JSON` must be the *entire* JSON on a single line:
```bash
jq -c . ~/.config/squashgo/firebase-sa.json
```

When deploying, the same values move to Supabase Secrets:
```bash
supabase secrets set --env-file supabase/functions/.env
supabase functions deploy send-notification --no-verify-jwt
```

## 6. Implementation order (one PR per step, mergeable independently)

**Progress as of 2026-05-10**: steps 1–5 shipped end-to-end (smoke-tested: curl → cloud send-notification → device renders localized notification). Next: step 6.

1. ✅ **Migration 007** — `push_tokens` schema fix (UNIQUE on token, last_seen_at, locale, UPDATE policy). Applied on cloud.
2. ✅ **Edge Function `send-notification`** — deployed to cloud with secrets configured. Structured error codes (no string-matching). Returns `{success, delivered, pruned, error?}`.
3. ✅ **Android skeleton** — Firebase BOM + messaging-ktx, `google-services.json` in `:app/`, `SquashGoMessagingService` registered in manifest, `POST_NOTIFICATIONS` permission declared.
4. ✅ **`PushTokenApi` + `PushTokenRepository` + DI wiring**: `FcmTokenProvider` interface in `:core:auth`, impl in `:app`. `AppBootstrapViewModel` registers token on Ready and DELETEs on logout (before `clearSession`).
5. ✅ **`NotificationRenderer` + channel + strings (en/es)** — `NotificationType` enum lives in `:core:model` (KMP-ready for iOS). Channel `match_play` registered from `SquashGoApplication.onCreate`. Renderer handles all 8 types, falls back to `notif_fallback_opponent` when name field missing.
6. ⏳ **Next — Migration 008: challenges trigger**. Postgres trigger AFTER INSERT/UPDATE on `challenges` that calls `send-notification` via `pg_net.http_post`. Needs `app.send_notification_url` + `app.internal_fn_secret` as Postgres custom config (one-time seed in dashboard). End-to-end test with two real accounts.
7. **Migration 009 — matches trigger (`result_submitted`)** + `MATCH_RESULT_SUBMITTED` rendering + deep link.
8. **Extend `confirm-match`, `dispute-match`, `auto_accept_pending_matches`** to fire `MATCH_CONFIRMED` / `MATCH_DISPUTED` / `MATCH_AUTO_CONFIRMED`.
9. **Deep-link handler** for the new push schemes in `MainActivity`.
10. **POST_NOTIFICATIONS permission UX** + rejection handling (today granted manually via `adb shell pm grant ...`).
11. **QA pass** — see §8.

## 7. Security & failure modes

- **Internal secret leakage** — anyone hitting `send-notification` with the secret could spam users. Treat it like service-role: never ship to client, never log, rotate via dashboard if exposed.
- **Token leakage in logs** — never log `token`. Edge function should hash before logging if needed.
- **Trigger backpressure** — `pg_net` is async and bounded; a backlog under heavy load will surface as delayed (not lost) notifications. Acceptable for MVP volume; revisit if we see queue depth.
- **Cold-start race** — `onNewToken` may fire before Hilt is ready. Use `WorkManager` (deferred) rather than calling the repository from the service directly.
- **Logout** — must `DELETE FROM push_tokens WHERE token = ?` before clearing the session, otherwise the token outlives the auth context and continues to receive pushes. Wire into `AppBootstrapViewModel.logout()`.

## 8. QA matrix (manual, two-device)

| # | Scenario | Expected |
|---|---|---|
| 1 | A sends challenge to B (B's app backgrounded) | B sees push within ~5s; tap opens ChallengeDetail |
| 2 | A sends challenge to B (B's app foregrounded on Discover) | No system notif; in-app indicator updates challenges count |
| 3 | B accepts challenge | A gets push within ~5s; tap opens ChallengeDetail |
| 4 | A submits match result | B gets push with priority=high; tap opens MatchConfirm |
| 5 | B confirms result | A gets push; opens MatchDetail; Profile shows updated Elo on next refresh |
| 6 | B disputes | A gets push; MatchDetail shows disputed state |
| 7 | A submits result, neither acts for 72h (use cron + system clock skew on staging) | Both A and B get auto-confirmed push |
| 8 | A logs out then logs back in on same device | Push to A still works (token re-registered) |
| 9 | A logs out, B logs in on same device | Pushes for old A's challenges are NOT delivered to B |
| 10 | App killed, push received | Tap opens app to correct deep-link route, NavBackStack contains parent route |
| 11 | Locale switched to ES | Notification title/body localized regardless of server-provided language |
| 12 | Token rotation (uninstall/reinstall) | Old `push_tokens` row replaced by upsert; no duplicate rows |
| 13 | Permission denied (Android 13+) | App still functions; in-app indicators still update; no crash |

## 9. Open questions / parking lot

- **Per-client white-label**: each white-label app will need its own Firebase project + `google-services.json`. Plumb `FCM_PROJECT_ID` + service account into per-client config. Aligns with `:app` as the customization surface (`docs/white-label.md`).
- **Quiet hours / per-user mute** — defer to post-MVP. Add a `notification_preferences` table when needed.
- **iOS** — the server side is identical. iOS work = APNs cert in Firebase console, `FIRMessaging` registration in SwiftUI app, mirror of `NotificationRenderer` using `UNNotificationContent`.
- **Analytics** — once Firebase Analytics lands (Phase 3), log `notification_received` / `notification_opened` automatically via Firebase Messaging hooks.
