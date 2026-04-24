# Challenges feature — handoff (2026-04-21)

Working doc for the in-progress Challenges/Play feature. Delete when feature is shipped.

## Status: implementation complete, NOT wired to entry points yet

- Full build green: `./gradlew :app:compileDebugKotlin`
- Branch: `develop`, uncommitted changes
- No tests written yet

## Design decisions (locked with Erik)

1. **MatchType default** in `ChallengeCreateScreen` → `CASUAL`
2. **Challenge message**: optional, max 200 chars (enforced in VM via `take(200)`)
3. **Outgoing ACCEPTED challenges**: shown in `ChallengesScreen` with label "Match in progress — result coming soon" (not clickable, no nav yet)
4. **Player+rating lookup**: batched `getPlayersWithRatings(ids)` added to `PlayerRepository` (two parallel PostgREST `in.(...)` calls, zipped by id)
5. **Status label**: `AssistChip` on every challenge card showing `PENDING` / `ACCEPTED` / etc. Days-until-expiry only on detail screen (plurals).
6. **ChallengesScreen `onMatchResult` / `onMatchConfirm` callbacks**: kept alive (suppressed UNUSED_VARIABLE) for future Active Matches wiring. Do not remove.
7. **Cancel outgoing PENDING**: `AlertDialog` confirmation → VM call → snackbar on error.
8. **Accept/Decline incoming**: happens on new `ChallengeDetailScreen` (route `ChallengeDetailRoute(challengeId)`), not inline.
9. **ChallengesScreen tabs**: Incoming | Outgoing (plain `TabRow` — has a deprecation warning, migrate to `PrimaryTabRow` when convenient).

## Files touched (all uncommitted)

**Domain/data:**
- `data/network/.../api/PlayerApi.kt` — added `getPlayersByIds`, `getRatingsByPlayerIds`
- `core/domain/.../repository/PlayerRepository.kt` — added `getPlayerWithRating`, `getPlayersWithRatings`
- `data/network/.../repository/SupabasePlayerRepository.kt` — implementations with `coroutineScope` + `async`

**Play feature (new):**
- `feature/play/impl/src/main/res/values/strings.xml` — new file, all user-facing strings
- `feature/play/impl/.../ChallengeCreateViewModel.kt` — new
- `feature/play/impl/.../ChallengeCreateScreen.kt` — full rewrite from stub
- `feature/play/impl/.../ChallengesViewModel.kt` — new
- `feature/play/impl/.../ChallengesScreen.kt` — full rewrite from stub, new `onIncomingTap` param
- `feature/play/impl/.../ChallengeDetailViewModel.kt` — new
- `feature/play/impl/.../ChallengeDetailScreen.kt` — new
- `feature/play/impl/.../navigation/PlayNavigation.kt` — wired `ChallengeDetailRoute`, added `onIncomingTap` to ChallengesScreen call
- `feature/play/api/.../PlayRoutes.kt` — added `ChallengeDetailRoute(challengeId)`
- `feature/play/impl/build.gradle.kts` — added `implementation(projects.core.auth)`

## Patterns used (match rest of codebase)

- `@HiltViewModel` + `sealed interface ...UiState` with `Loading` / `Ready(...)` / `Done|Sent` / `Error(err)` variants
- One-shot success: VM sets state to a terminal `Done`/`Sent` data object, Screen has `LaunchedEffect(uiState) { if (state is X) onX() }`
- Errors as enum on the Error state OR as a boolean transient flag on Ready (e.g. `cancelError: Boolean`, `actionError: Boolean`)
- All user-facing strings in strings.xml (per memory rule)
- `try/catch(CancellationException) { throw e } catch(Throwable)` in every `launch`
- Elo display via `core/domain/.../rating/TierMapper.displayStringForRating()` — but I wrote a small Composable `tierDisplay(PlayerWithRating)` in `ChallengeCreateScreen.kt` that's also used by `ChallengesScreen.kt` and `ChallengeDetailScreen.kt` (same package).

## Known gaps / follow-ups

### Blocking user flow
1. **No entry point to `ChallengeCreateRoute`** — needs a "Challenge" button on `CourtDetailScreen` player rows. Passes `challengedId = player.id`, `courtId = court.id`.
2. **No entry point to `ChallengesRoute`** — needs a top-level nav destination (bottom nav / drawer / home).

### Nice to have
- Migrate `TabRow` → `PrimaryTabRow` in `ChallengesScreen.kt:104` (deprecation warning, not an error).
- `SupabaseCourtRepository.getCourtWithPlayers` still does N+1 for ratings; could migrate to the new batched method.
- No tests yet. Candidates: `ChallengeCreateViewModel` (happy path + 200-char truncation + send error), `ChallengesViewModel` (cancel success + cancel error), `ChallengeDetailViewModel.computeExpiry()`.

### Out of scope (explicitly deferred)
- Active Matches tab / navigation from ACCEPTED outgoing to MatchResult/MatchConfirm. Callbacks `onMatchResult`/`onMatchConfirm` are wired in `PlayNavigation.kt` but not invoked from the screen.

## How to pick this back up

1. Read this file.
2. `git status` to see the uncommitted surface.
3. Decide: wire entry points (CourtDetail + home nav) or write tests first?
4. If entry points: `CourtDetailScreen.kt` player rows already exist; add a "Challenge" `TextButton` per row that calls `onChallengePlayer(playerId, courtId)`, bubble up through `DiscoverNavigation.kt` to navigate `ChallengeCreateRoute(challengedId, courtId)`.