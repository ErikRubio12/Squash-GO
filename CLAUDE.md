# Squash & Go - Claude Code Guide

## Project Overview
Squash & Go is a mobile app for squash players to discover courts, find rivals, challenge each other, record match results, and build Elo-style skill ratings. Pilot market: Metro Vancouver.

## Tech Stack
- **Language**: Kotlin 2.3.0
- **KMP Modules**: `:core:model`, `:core:domain`, `:data:network` — models, domain logic, Ktor networking (shared with future iOS)
- **Android UI**: Jetpack Compose + Material3
- **iOS UI**: SwiftUI (future — KMP modules already prepared)
- **DI**: Hilt 2.58 + KSP
- **Backend**: Supabase (Postgres, Auth, Edge Functions) — accessed via REST APIs using Ktor
- **Navigation**: Navigation Compose 2.9.5 with type-safe serializable routes
- **Push**: FCM (Android), APNs via FCM (iOS future)
- **Analytics**: Firebase Analytics (Phase 3)

## Module Structure

### App
- `:app` — Android app entry point, Hilt DI wiring, NavHost, Supabase config

### Core (shared infrastructure)
- `:core:model` — KMP. Domain entities (Player, Court, Match, Challenge, Rating, Session, Enums)
- `:core:domain` — KMP. Repository interfaces, pure business logic (EloCalculator, TierMapper, ScoreValidator)
- `:core:common` — Android. Dispatchers, shared utilities
- `:core:designsystem` — Android Compose. SquashGoTheme, colors
- `:core:navigation` — Android. Navigation-compose dependency anchor
- `:core:auth` — Android + Hilt. SessionManager (access token, user ID)
- `:core:ui` — Android Compose. Reusable domain-aware composables (future)

### Data
- `:data:network` — KMP. Ktor API layer, DTOs, mappers, Supabase repository implementations
- `:data:database` — Android. Local persistence (future)

### Features (api/impl split)
Each feature has two modules:
- `:feature:<name>:api` — `@Serializable` route classes only (public contract)
- `:feature:<name>:impl` — Screens, ViewModels, `NavGraphBuilder` extension (private implementation)

Features: `onboarding`, `discover`, `play`, `profile`, `activity`

### Backend
- `/supabase` — Database migrations, seed data, Edge Functions

## Convention Plugins (build-logic/)
- `squashgo.kmp.library` — KMP + Android lib + iOS targets + serialization
- `squashgo.android.library` — Android lib with SDK config
- `squashgo.android.library.compose` — Android lib + Compose
- `squashgo.android.feature` — Compose + Hilt + auto-deps on core modules
- `squashgo.android.application` — Android app config
- `squashgo.hilt` — KSP + Hilt plugin + deps

## Architecture Pattern
```
Screen -> ViewModel -> Repository Interface (:core:domain)
                            -> Repository Impl (:data:network)
                                -> *Api class (:data:network)
                                    -> Ktor HttpClient
                                        -> Supabase REST API
```

DI wiring lives in `:app/di/` (NetworkModule, ApiModule, RepositoryModule).
Hilt modules use `@Provides` (not `@Binds`) for repository bindings since impls are KMP.

## File Naming
- Screens: `*Screen.kt`
- ViewModels: `*ViewModel.kt`
- Repository interfaces: `*Repository.kt` (in `:core:domain`)
- Repository implementations: `Supabase*Repository.kt` (in `:data:network`)
- API classes: `*Api.kt` (in `:data:network`)
- DTOs: `*Dto.kt` (in `:data:network`)
- Mappers: `*Mapper.kt` (in `:data:network`)
- Domain models: singular noun (Player.kt, Court.kt)
- Feature routes: `Routes.kt` (in `:feature:<name>:api`)
- Feature navigation: `*Navigation.kt` with `NavGraphBuilder.<feature>Graph()` extension

## Code Organization — data classes in `model/` subpackages

**Rule:** pure `data class`, `sealed interface`, `enum class`, and other type-only declarations MUST live in a `model/` subpackage inside their own layer. Classes that contain behavior (ViewModels, Screens, Repositories, Api classes, Mappers) keep only their own code — no co-located UI state, errors, rows, DTOs, etc.

### Where each kind lives
| Kind | Layer / module | Location |
|---|---|---|
| Domain entities (`Player`, `Court`, `Match`, `Rating`, enums) | `:core:model` | `core/model/.../` (already flat — this *is* the model package) |
| Repository interface helpers (`PlayerWithRating`, domain result types) | `:core:domain` | `core/domain/.../repository/model/` |
| UI state, UI errors, row/card presentation types, one-shot effects | `:feature:<name>:impl` | `feature/<name>/impl/.../model/` |
| DTOs (`*Dto`) | `:data:network` | `data/network/.../dto/` (existing convention — keep) |
| Mappers (`*Mapper`) | `:data:network` | `data/network/.../mapper/` (existing convention — keep) |
| Route classes (`@Serializable`) | `:feature:<name>:api` | `feature/<name>/api/.../` (flat, it *is* the contract) |

### Example: feature/impl

```
feature/activity/impl/.../
├── MatchHistoryScreen.kt           # @Composable only
├── MatchHistoryViewModel.kt        # @HiltViewModel only
├── model/
│   └── MatchHistoryUiState.kt      # sealed interface UiState, MatchRow, MatchHistoryError enum
└── navigation/
    └── ActivityNavigation.kt
```

**Why:** keeps ViewModels and Screens focused on behavior, makes state types greppable by name (`MatchHistoryUiState` vs scrolling through a 400-line VM), and prevents accidental coupling between presentation state and business logic.

### Reusable Compose helpers
Cross-feature Compose utilities (e.g. `currentLocale()`, domain-aware composables) live in `:core:ui` under a semantic subpackage (`core/ui/.../locale/`, `core/ui/.../match/`, etc.). The `squashgo.android.feature` convention plugin already adds `:core:ui` as an `implementation` dep.

## Domain Rules

### Challenge States
`PENDING -> ACCEPTED/DECLINED/CANCELLED/EXPIRED -> COMPLETED`

### Match States
`IN_PROGRESS -> RESULT_SUBMITTED -> CONFIRMED/DISPUTED -> (admin resolves)`

### Rating System
- Elo formula: E = 1/(1+10^((Rb-Ra)/400)), R' = R + K*(S-E)
- K-factors: 60 (both provisional), 40 (one provisional), 32 (both established)
- Tiers: Bronze(800-999), Silver(1000-1199), Gold(1200-1399), Platinum(1400-1599), Diamond(1600-1799), Master(1800+)
- Each tier has 5 divisions (40 Elo each). Division 5 = lowest, Division 1 = highest.
- Starting Elo: 1200 (Gold 5). Provisional: first 5 ranked matches.
- Only ranked matches affect rating. Casual matches are recorded but have no rating impact.

### Score Validation
- Best of 5 games. Winner needs 3 games.
- Game won at 11+ points, win by 2 in deuce.
- Result confirmed when both players agree or 72h auto-accept.

## Supabase Conventions
- Migrations: `supabase/migrations/` (numbered SQL files)
- Edge Functions: `supabase/functions/<name>/` (Deno/TypeScript)
- RLS enabled on ALL tables. Service role used only in Edge Functions.
- PostgREST query patterns: `parameter("field", "eq.value")` for filtering

## Common Tasks

### Add a new screen
1. Add `@Serializable` route to `:feature:<name>:api/Routes.kt`
2. Create `<Name>Screen.kt` in `:feature:<name>:impl`
3. Create `<Name>ViewModel.kt` with `@HiltViewModel` if needed
4. Add `composable<Route>` to the feature's `NavGraphBuilder` extension in `*Navigation.kt`

### Add a new feature module
1. Create `:feature:<name>:api` with `Routes.kt` (apply `squashgo.android.library`)
2. Create `:feature:<name>:impl` with screens + navigation (apply `squashgo.android.feature`)
3. Add both to `settings.gradle.kts`
4. Add impl dependency in `:app/build.gradle.kts`
5. Call `<feature>Graph()` in `SquashGoApp.kt` NavHost

### Add a Supabase table
1. Create new migration file in `supabase/migrations/`
2. Add RLS policies
3. Create DTO in `:data:network/.../dto/`
4. Create/update mapper in `:data:network/.../mapper/`
5. Update repository interface (`:core:domain`) and implementation (`:data:network`)

### Add an Edge Function
1. Create directory `supabase/functions/<name>/`
2. Add `index.ts` with Deno handler
3. Add API call in `EdgeFunctionApi.kt` (`:data:network`)

## What Not to Do
- No shared Compose UI across platforms — Jetpack Compose for Android, SwiftUI for iOS
- No Supabase SDK — use Ktor + REST APIs directly
- No rating calculation on the client — rating updates happen server-side in Edge Functions
- No `@Binds` for repository bindings — use `@Provides` since impls are KMP (no `@Inject` constructors)
- Feature `:impl` modules must NOT depend on other features' `:impl` — only on `:api` modules
