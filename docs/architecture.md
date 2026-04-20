# Architecture Blueprint

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           :app                                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ SquashGoApp.kt   │  │ MainActivity.kt  │  │ di/              │   │
│  │ (NavHost)        │  │ @AndroidEntryPoint│  │ NetworkModule    │   │
│  │                  │  │                  │  │ ApiModule        │   │
│  │                  │  │                  │  │ RepositoryModule │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
└───────────────┬─────────────────────────────────────────────────────┘
                │ depends on all feature, core, and data modules
┌───────────────▼─────────────────────────────────────────────────────┐
│                    Feature Modules (api/impl split)                   │
│                                                                       │
│  :feature:onboarding:api  ──── :feature:onboarding:impl              │
│  :feature:discover:api    ──── :feature:discover:impl                │
│  :feature:play:api        ──── :feature:play:impl                    │
│  :feature:profile:api     ──── :feature:profile:impl                 │
│  :feature:activity:api    ──── :feature:activity:impl                │
│                                                                       │
│  api = @Serializable route classes only                              │
│  impl = Screens + ViewModels + NavGraphBuilder extension             │
│  impl depends on own :api + other features' :api (navigation only)  │
└───────────────┬─────────────────────────────────────────────────────┘
                │
┌───────────────▼─────────────────────────────────────────────────────┐
│                        Core Modules                                   │
│                                                                       │
│  :core:model (KMP)        Domain entities, enums                     │
│  :core:domain (KMP)       Repository interfaces, business logic      │
│  :core:common             Dispatchers, shared utilities              │
│  :core:designsystem       SquashGoTheme, Material3 palette           │
│  :core:navigation         Navigation-compose dependency anchor       │
│  :core:auth               SessionManager (Hilt singleton)            │
│  :core:ui                 Reusable domain-aware composables          │
└───────────────┬─────────────────────────────────────────────────────┘
                │
┌───────────────▼─────────────────────────────────────────────────────┐
│                        Data Modules                                   │
│                                                                       │
│  :data:network (KMP)      Ktor APIs, DTOs, mappers, repo impls      │
│  :data:database           Local persistence (future)                 │
└───────────────┬─────────────────────────────────────────────────────┘
                │
           Ktor HTTP
                │
┌───────────────▼─────────────────────────────────────────────────────┐
│                    Supabase Backend                                    │
│                                                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐      │
│  │  PostgREST   │  │  Auth (GoTrue│  │  Edge Functions       │      │
│  │  (CRUD API)  │  │  magic link) │  │  confirm-match        │      │
│  └──────┬───────┘  └──────────────┘  └───────────┬───────────┘      │
│         │                                         │                  │
│  ┌──────▼─────────────────────────────────────────▼───────────┐     │
│  │                    PostgreSQL                               │     │
│  │  players · courts · player_courts · ratings · challenges   │     │
│  │  matches · disputes · rating_history · push_tokens         │     │
│  │                                                             │     │
│  │  RLS policies on ALL tables                                │     │
│  │  Triggers: handle_new_user, update_*_updated_at            │     │
│  └────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
```

## Module Dependency Graph

```
:app -> :feature:*:impl -> own :api, other features' :api
                        -> :core:model, :core:ui, :core:designsystem, :core:navigation
                        -> :core:domain, :core:auth

:app -> :data:network, :data:database
:app -> :core:* (all core modules)

:data:network -> :core:model, :core:domain
:core:domain  -> :core:model
:core:ui      -> :core:model, :core:designsystem
:core:auth    -> :core:common
```

## Module Breakdown

### :app
```
app/src/main/kotlin/com/egr/squashgo/
├── SquashGoApplication.kt       # @HiltAndroidApp entry point
├── MainActivity.kt              # @AndroidEntryPoint, calls SquashGoApp()
├── ui/
│   └── SquashGoApp.kt           # NavHost composable, calls all feature graphs
└── di/
    ├── NetworkModule.kt          # Provides HttpClient with tokenProvider
    ├── ApiModule.kt              # Provides all *Api instances
    └── RepositoryModule.kt       # @Provides binding repo interfaces to Supabase impls
```

### :core:model (KMP)
```
core/model/src/commonMain/kotlin/com/egr/squashgo/core/model/
├── Player.kt                    # id, displayName, email, avatarUrl, availability
├── Court.kt                     # id, name, address, lat/lng, numberOfCourts
├── Match.kt                     # full lifecycle + MatchScore/GameScore
├── Challenge.kt                 # challenger/challenged, status, dates
├── Rating.kt                    # eloScore, isProvisional + RatingHistory
├── Dispute.kt                   # matchId, reason, resolution
├── PlayerCourt.kt               # M2M: playerId <-> courtId
├── Session.kt                   # accessToken, refreshToken, userId, email
└── Enums.kt                     # MatchType, ChallengeStatus, MatchStatus, Tier...
```

### :core:domain (KMP)
```
core/domain/src/commonMain/kotlin/com/egr/squashgo/core/domain/
├── repository/                   # Interfaces (contracts)
│   ├── AuthRepository.kt
│   ├── PlayerRepository.kt
│   ├── CourtRepository.kt
│   ├── MatchRepository.kt
│   ├── ChallengeRepository.kt
│   └── RatingRepository.kt
├── rating/
│   ├── EloCalculator.kt         # K=60/40/32, floor 400
│   └── TierMapper.kt            # Elo -> Tier/Division
└── match/
    └── ScoreValidator.kt         # Best-of-5, 11pts, deuce rules
```

### :core:common
```
core/common/src/main/kotlin/com/egr/squashgo/core/common/
├── SquashGoDispatchers.kt       # @Qualifier annotation for IO/Default
└── DispatchersModule.kt         # Hilt @Module providing dispatchers
```

### :core:designsystem
```
core/designsystem/src/main/kotlin/com/egr/squashgo/core/designsystem/
└── theme/
    └── Theme.kt                  # Material3 green palette (light/dark)
```

### :core:auth
```
core/auth/src/main/kotlin/com/egr/squashgo/core/auth/
└── SessionManager.kt            # @Singleton: accessToken, currentUserId, login/logout
```

### :data:network (KMP)
```
data/network/src/commonMain/kotlin/com/egr/squashgo/data/network/
├── api/                          # Ktor HTTP layer
│   ├── SupabaseApi.kt            # HttpClient factory (tokenProvider lambda)
│   ├── AuthApi.kt                # magicLink, verifyOtp, refreshToken
│   ├── PlayerApi.kt              # CRUD + rating lookup
│   ├── CourtApi.kt               # getCourts, getCourtWithPlayers
│   ├── ChallengeApi.kt           # create, list, updateStatus
│   ├── MatchApi.kt               # create, submitResult, history
│   └── EdgeFunctionApi.kt        # confirmMatch (server-side rating update)
├── dto/                          # @Serializable JSON models
│   ├── AuthDto.kt
│   ├── PlayerDto.kt
│   ├── CourtDto.kt
│   ├── MatchDto.kt
│   ├── ChallengeDto.kt
│   └── RatingDto.kt
├── mapper/                       # DTO <-> Domain conversion
│   ├── AuthMapper.kt
│   ├── PlayerMapper.kt
│   ├── CourtMapper.kt
│   ├── MatchMapper.kt
│   ├── ChallengeMapper.kt
│   └── RatingMapper.kt
└── repository/                   # Supabase implementations
    ├── SupabaseAuthRepository.kt
    ├── SupabasePlayerRepository.kt
    ├── SupabaseCourtRepository.kt
    ├── SupabaseMatchRepository.kt
    ├── SupabaseChallengeRepository.kt
    └── SupabaseRatingRepository.kt
```

### Feature Modules (api/impl pattern)

Each feature follows this structure:

```
feature/<name>/api/src/main/kotlin/.../
└── Routes.kt                    # @Serializable route data classes

feature/<name>/impl/src/main/kotlin/.../
├── <Name>Screen.kt              # @Composable UI
├── <Name>ViewModel.kt           # @HiltViewModel (when needed)
└── navigation/
    └── <Name>Navigation.kt      # fun NavGraphBuilder.<name>Graph(...)
```

**Features:**
- `onboarding` — LoginRoute, LoginScreen (magic link auth)
- `discover` — CourtListRoute, CourtDetailRoute(courtId), court browsing screens
- `play` — ChallengeCreate/Challenges/MatchResult/MatchConfirm routes + screens
- `profile` — ProfileRoute, user profile screen
- `activity` — MatchHistoryRoute, past matches screen

### /supabase (Backend)
```
supabase/
├── migrations/
│   └── 001_initial.sql           # Full schema, RLS, triggers, indexes
├── seed.sql                      # 10 Metro Vancouver courts
└── functions/
    └── confirm-match/
        └── index.ts              # Deno: confirm result + update ratings
```

## Convention Plugins (build-logic/)

| Plugin ID | Base | Adds |
|---|---|---|
| `squashgo.kmp.library` | — | KMP + Android lib + iOS targets + serialization |
| `squashgo.android.library` | — | Android lib, SDK config, Java/Kotlin 11 |
| `squashgo.android.library.compose` | `android.library` | Compose Multiplatform + compiler |
| `squashgo.android.feature` | `compose` + `hilt` | Auto-deps on core:model/ui/designsystem/navigation + lifecycle + nav |
| `squashgo.android.application` | — | Android app, targetSdk, Kotlin 11 |
| `squashgo.hilt` | — | KSP + Hilt plugin + hilt-android + hilt-compiler |

## Data Flow

### Read Path
```
Screen -> ViewModel -> Repository.getX()  [:core:domain interface]
  -> SupabaseXRepository.getX()           [:data:network impl]
    -> XApi.getX()  [Ktor GET]
      -> XDto (deserialized)
    -> XMapper.toDomain(dto)
  -> Domain Model -> ViewModel StateFlow -> UI
```

### Write Path
```
Screen -> ViewModel -> Repository.doX()   [:core:domain interface]
  -> SupabaseXRepository.doX()            [:data:network impl]
    -> XApi.postX(RequestDto)  [Ktor POST/PATCH]
      -> Response DTO
    -> XMapper.toDomain(response)
  -> Domain Model -> ViewModel StateFlow -> UI
```

### Match Confirmation (Edge Function)
```
MatchConfirmScreen -> ViewModel -> MatchRepository.confirmMatch(matchId)
  -> SupabaseMatchRepository.confirmMatch()
    -> EdgeFunctionApi.confirmMatch()  [POST /functions/v1/confirm-match]
      -> Edge Function (Deno):
         1. Validates both players confirmed
         2. Calculates new Elo ratings (server-side)
         3. Updates ratings table
         4. Creates rating_history entries
         5. Sets match status = CONFIRMED
```

## Database Schema

```
players ──────< player_courts >────── courts
   │                                     │
   ├──< ratings                          │
   │                                     │
   ├──< challenges (as challenger) ──────┘
   ├──< challenges (as challenged)
   │
   ├──< matches (as player_a)
   ├──< matches (as player_b)
   │       │
   │       ├──< disputes
   │       │
   │       └── -> confirm-match edge function
   │                    │
   └──< rating_history <┘
```

**Tables:** players, courts, player_courts, ratings, challenges, matches, disputes, rating_history, push_tokens

**Security:** RLS enabled on all tables. Service role used only in Edge Functions.

**Triggers:**
- `handle_new_user()` — Auto-creates player + rating on auth signup
- `update_*_updated_at()` — Timestamp management

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Module architecture | NiA-inspired api/impl split | White-labeling support, clean boundaries |
| DI | Hilt 2.58 + KSP | Industry standard, convention plugin friendly |
| Repo bindings | `@Provides` not `@Binds` | Repo impls are KMP (no `@Inject` in commonMain) |
| Hilt modules location | `:app` not `:data:network` | Keeps data layer pure KMP for iOS |
| HTTP Client | Ktor (no Supabase SDK) | Full control, KMP compatible |
| Token injection | Lambda `tokenProvider: () -> String?` | Hilt singleton can't be recreated per session |
| Navigation | Nav Compose 2.9.5 + serializable routes | Type-safe, works with api/impl split |
| State | StateFlow | Standard, collected by Compose |
| Serialization | kotlinx-serialization | KMP-native, works with Ktor |
| Rating calc | Server-side only (Edge Function) | Prevents client tampering |
| RLS | All tables | Security at DB layer |
| UI per platform | Compose (Android) / SwiftUI (iOS) | Native feel, shared logic in KMP modules |
| Match auto-accept | 72h timeout | Prevents confirmation deadlock |
| Score validation | In `:core:domain` | Consistent rules across platforms |

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.3.0 |
| Android UI | Jetpack Compose + Material3 | 1.10.0 |
| Multiplatform | Kotlin Multiplatform | 2.3.0 |
| DI | Hilt + KSP | 2.58 / 2.3.0 |
| HTTP Client | Ktor | 3.1.3 |
| Serialization | Kotlinx Serialization | 1.8.1 |
| Date/Time | Kotlinx DateTime | 0.6.2 |
| Async | Kotlinx Coroutines | 1.10.2 |
| Navigation | Navigation Compose | 2.9.5 |
| Backend | Supabase (Postgres + PostgREST + Auth) | -- |
| Edge Functions | Deno (TypeScript) | -- |
| AGP | Android Gradle Plugin | 8.11.2 |
| Min Android SDK | 31 (Android 12) | -- |
| Target/Compile SDK | 36 | -- |

## KMP / iOS Readiness

Three modules are KMP-ready for future iOS (SwiftUI) integration:
- `:core:model` — domain entities
- `:core:domain` — repository interfaces + business logic
- `:data:network` — Ktor API layer + repository implementations

iOS targets configured: `iosArm64`, `iosSimulatorArm64`

iOS app would:
1. Add `:iosApp` module with SwiftUI
2. Consume KMP framework from `:data:network` (which transitively includes model + domain)
3. Use its own DI (no Hilt on iOS)
