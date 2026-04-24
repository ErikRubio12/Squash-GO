# Shell feature + i18n + Activity impl — handoff (2026-04-22)

Working doc for the MainShell bottom-nav feature. Delete when shipped.

## Goal

Add a bottom NavigationBar shell for post-login top-level destinations (Discover, Play, Activity, Profile), implement the Activity screen (currently stub), and add Spanish locale support across all existing strings.

## Status: Steps 1 & 2 done, ready for step 3

- Step 1 ✅ committed (`3778e69`) and pushed — `:feature:shell:api` with `TopLevelDestination` sealed interface
- Step 2 ✅ committed locally (not pushed yet) — `:feature:shell:impl` with `MainShellScaffold`, strings EN+ES, `TopLevelDestination` wired end-to-end into the tab spec so adding a 5th tab will force a compiler error until the `when` is updated
- Icons: used `LocationOn` / `PlayArrow` / `AutoMirrored.List` / `Person` from `material-icons-core` (deviation from plan which asked for `SportsBaseball` + `History` — Erik chose not to add `material-icons-extended` for MVP)
- Not yet wired: `MainShellScaffold` is unused; `SquashGoApp.kt` still runs the raw `NavHost` without the scaffold wrapper — that's step 3

## Status (original): NOT started — plan locked with Erik, ready to execute step by step

- Branch: `develop`, uncommitted changes from `challenges-feature.md` still pending (Challenges feature implementation-complete but not wired to entry points)
- After shell lands, Challenges gets its entry point via the Play tab automatically

## Design decisions (locked with Erik)

### Shell architecture: Scaffold wrapper, no nested NavHost

The shell is a thin `Scaffold` wrapper around the existing outer `NavHost`. `NavigationBar` is shown **conditionally** based on whether the current destination is a top-level route.

- ✅ Zero refactor to existing feature graphs (`discoverGraph`, `playGraph`, `activityGraph`, `profileGraph`)
- ✅ Detail screens hide the bar automatically (their routes aren't in the top-level set)
- ✅ Each tab preserves its back stack via `popUpTo(startDest) { saveState = true } + restoreState = true`
- ❌ Rejected: nested NavHost per tab (too much refactor, no clear benefit for MVP)

### Tabs (left→right, Material3 NavigationBar)

| Tab | Route (existing) | Icon |
|---|---|---|
| Discover | `CourtListRoute` (feature/discover/api) | `Icons.Default.LocationOn` |
| Play | `ChallengesRoute` (feature/play/api) | `Icons.Default.SportsBaseball` |
| Activity | `MatchHistoryRoute` (feature/activity/api) | `Icons.Default.History` |
| Profile | `ProfileRoute` (feature/profile/api — verify name) | `Icons.Default.Person` |

Start tab post-login: **Discover**.

### Locale: Spanish (ES) added alongside English (EN)

- System locale driven (no in-app picker — deferred)
- Every `values/strings.xml` gets a `values-es/strings.xml` sibling
- Default values in `values/` stay EN

### Activity screen: real implementation, not stub

`MatchHistoryScreen` reads real data:
- `MatchRepository.getMatchesForPlayer(currentUserId)` — already exists
- Per match: fetch opponent via `PlayerRepository.getPlayersWithRatings(ids)` (batched, already exists)
- Row: opponent name · W/L · games summary (e.g. "3-1") · relative date
- Empty state: "No matches yet — challenge someone!"
- Loading / Error states standard (`sealed UiState`)

Filter: show **only CONFIRMED matches** (ignore IN_PROGRESS / DISPUTED for V0 — flagged as follow-up).

## Scope explicitly OUT

- In-app locale picker
- Badge counts on tabs (structure ready, not populated)
- MainShell ViewModel (no state needed yet)
- Activity detail screen (tap = no-op for V0)
- Fixing pre-existing bug: `AppBootstrapViewModel` treats `LoggedIn` as "go to ProfileSetupRoute every time", forcing onboarding on every cold start for users already onboarded. See `Known gaps` below.

## Step-by-step execution plan

Execute one step at a time, confirm with Erik between steps.

### Step 1 — Create `:feature:shell:api` module
- Create `feature/shell/api/build.gradle.kts` (mirror `feature/discover/api/build.gradle.kts`)
- Create `feature/shell/api/src/main/AndroidManifest.xml` (empty manifest)
- Create `feature/shell/api/src/main/kotlin/com/egr/squashgo/feature/shell/api/TopLevelDestination.kt`:
  ```kotlin
  // enum with 4 entries, each holding its route object reference
  // Note: enum can't hold @Serializable directly — use sealed interface + data objects
  //       OR a simple enum + a separate route-resolver function in :impl
  ```
- Decision: **sealed interface with data objects** is cleanest because routes are already `@Serializable`:
  ```kotlin
  sealed interface TopLevelDestination {
      data object Discover : TopLevelDestination
      data object Play : TopLevelDestination
      data object Activity : TopLevelDestination
      data object Profile : TopLevelDestination
  }
  ```
  Then `:impl` has a mapper from `TopLevelDestination` → concrete route (to keep `:api` light of cross-feature deps).
- Add to `settings.gradle.kts`: `include(":feature:shell:api")`

### Step 2 — Create `:feature:shell:impl` module
- `feature/shell/impl/build.gradle.kts` with `squashgo.android.feature` plugin
- Dependencies:
  ```
  implementation(projects.feature.shell.api)
  implementation(projects.feature.discover.api)
  implementation(projects.feature.play.api)
  implementation(projects.feature.activity.api)
  implementation(projects.feature.profile.api)
  ```
- `feature/shell/impl/src/main/AndroidManifest.xml`
- `feature/shell/impl/src/main/res/values/strings.xml`:
  ```
  shell_tab_discover, shell_tab_play, shell_tab_activity, shell_tab_profile
  shell_tab_discover_cd, shell_tab_play_cd, shell_tab_activity_cd, shell_tab_profile_cd (content descriptions)
  ```
- `feature/shell/impl/src/main/res/values-es/strings.xml` — same keys in ES
- `feature/shell/impl/.../MainShellScaffold.kt`:
  ```kotlin
  @Composable
  fun MainShellScaffold(
      navController: NavController,
      content: @Composable (PaddingValues) -> Unit,
  )
  ```
  - Reads `currentBackStackEntryAsState`
  - Computes `shouldShowBar`: true if destination route matches any of the 4 top-level routes
  - Uses `destination.hasRoute<CourtListRoute>() || ...` pattern (type-safe routes)
  - Renders `NavigationBar` with 4 `NavigationBarItem`s
  - Tab tap: `navController.navigate(route) { popUpTo(startDestId) { saveState = true }; launchSingleTop = true; restoreState = true }`
- Add to `settings.gradle.kts`: `include(":feature:shell:impl")`

### Step 3 — Wire in `:app`
- `:app/build.gradle.kts`:
  ```
  implementation(projects.feature.shell.api)
  implementation(projects.feature.shell.impl)
  ```
- `SquashGoApp.kt` — wrap `NavHost` with `MainShellScaffold`:
  ```kotlin
  val navController = rememberNavController()
  MainShellScaffold(navController) { padding ->
      NavHost(
          navController = navController,
          startDestination = startDestination,
          modifier = Modifier.padding(padding),
      ) { ... existing graphs ... }
  }
  ```
- `onOnboardingComplete` callback stays as-is (`navigate(CourtListRoute) { popUpTo(HomeCourtPickerRoute) { inclusive = true } }`)

### Step 4 — Implement Activity (MatchHistory) for real
- `feature/activity/impl/build.gradle.kts`:
  ```
  implementation(projects.core.auth)  // for SessionManager.currentUserId
  implementation(projects.core.model)
  implementation(projects.core.domain)
  ```
- `feature/activity/impl/.../MatchHistoryViewModel.kt`:
  ```kotlin
  @HiltViewModel
  class MatchHistoryViewModel @Inject constructor(
      private val matchRepository: MatchRepository,
      private val playerRepository: PlayerRepository,
      private val sessionManager: SessionManager,
  ) : ViewModel() {
      sealed interface UiState {
          data object Loading : UiState
          data object Empty : UiState
          data class Ready(val rows: List<MatchRow>) : UiState
          data class Error(val kind: ErrorKind) : UiState
      }
      // MatchRow: opponentName, opponentElo, didWin, scoreDisplay, playedAtDisplay
  }
  ```
- `feature/activity/impl/.../MatchHistoryScreen.kt` — full rewrite:
  - TopAppBar with i18n title
  - `LazyColumn` of `MatchCard` composables
  - Empty/Loading/Error states
  - Date formatting: `java.time.LocalDate` + DateTimeFormatter via `Locale.getDefault()` → relative ("yesterday", "3 days ago") if ≤7d, else absolute
- `feature/activity/impl/src/main/res/values/strings.xml` — new file
- `feature/activity/impl/src/main/res/values-es/strings.xml` — new file
- Filter in VM: `matches.filter { it.status == MatchStatus.CONFIRMED }`

### Step 5 — Spanish locale for all existing strings
Add `values-es/strings.xml` to:
- `app/src/main/res/` — just `app_name` (translates to "SquashGo" — no change)
- `feature/onboarding/impl/src/main/res/` — translate all keys
- `feature/discover/impl/src/main/res/` — translate all keys
- `feature/play/impl/src/main/res/` — translate all keys (~40 strings — biggest chunk)

Translation notes:
- "Challenge" → "Reto" / "Desafío" (pick one — recommend "Reto" as shorter)
- "Match" → "Partido"
- "Elo" stays (proper noun)
- Tier names (Bronze, Silver, etc.) — check where they live, may stay in English as brand terms

### Step 6 — Build verification
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- Manual test plan (if Erik can sideload):
  - Cold start when logged in → lands on Discover with bottom bar
  - Tap each tab → correct screen, bar persists
  - Tap Discover → CourtDetail → bar hides
  - Back → bar reappears
  - Switch phone locale to ES → strings update on restart

## Known gaps / follow-ups (post-shell)

### Blocking product quality
1. **Bootstrap bug**: `AppBootstrapViewModel.LoggedIn` always routes to `ProfileSetupRoute`. Fix with `SessionManager.isOnboarded` flag (persisted when `HomeCourtPickerScreen.onCourtsSaved` fires), or with a `LoggedInOnboarded` bootstrap state that checks profile existence via network on cold start.
2. **Challenges entry point**: once shell lands, the Play tab is `ChallengesRoute` — this unblocks the challenges feature. The "Challenge" button on `CourtDetailScreen` player rows is still needed (see `challenges-feature.md`).

### Nice to have
- `MatchHistoryScreen` tap on a row → `MatchDetailScreen` (doesn't exist yet).
- Activity tab: filter chips for "Confirmed" / "Disputed" / "In Progress".
- Badge on Play tab showing incoming challenge count.
- In-app locale picker (per-app language, API 33+ `AppLocalesMetadata`).
- Material3 `ModalNavigationDrawer` for tablet/large-screen expanded layout.

## How to pick this back up

1. Read this file.
2. `git status` to confirm surface area (should still be uncommitted challenges + shell diffs once started).
3. If resuming mid-execution: check which step was last completed via file existence (`ls feature/shell/`).
4. Execute next step, show Erik the diff, wait for approval, continue.