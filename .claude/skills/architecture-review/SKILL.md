---
name: architecture-review
description: Review a change (uncommitted diff, commit, branch, specific files, or feature scope) for Squash & Go architecture rules: modularization, api/impl split, core/data/feature boundaries, repository ownership, DI placement, MVI consistency, and coroutine/structured concurrency practices.
---

# architecture-review

Use this skill when the user asks to review architecture, modularization, boundaries, layering, feature ownership, DI setup, MVI consistency, coroutine usage, or whether a change respects the project's module structure.

## Scope

Target one of:
- Uncommitted working tree (`git diff` + untracked)
- A specific commit or range (`git diff <base>..HEAD`)
- A list of files the user gives you
- The whole feature the user names ("review the onboarding feature")

Ask which one only if ambiguous. Default to uncommitted working tree.

## Primary architecture rules

### Module layering
- `:core:model` (KMP): no deps on other modules except `kotlinx-datetime` / `kotlinx-serialization`. Pure data classes + enums only.
- `:core:domain` (KMP): depends only on `:core:model`. Interfaces + pure business logic only. No Android imports, no Ktor, no Hilt.
- `:core:auth`, `:core:common`, `:core:designsystem`, `:core:navigation`, `:core:ui` (Android): may depend on `:core:*` only.
- `:data:network` (KMP): depends on `:core:model` + `:core:domain`. Owns Ktor, DTOs, mappers, API classes, and repository implementations.
- `:data:database` (Android, future): same layering expectations for persistence concerns.
- `:feature:<name>:api` (Android lib): route/navigation contracts only. No screens, no ViewModels, no impl deps.
- `:feature:<name>:impl` (Android feature): screens, ViewModels, and feature navigation graph wiring. May depend on `:core:*` and its own `:api`. Must not depend on other features' `:impl`.
- `:app`: composition root only. Owns DI modules and top-level NavHost wiring.

### Dependency direction
- Core modules may depend only on other core modules.
- Core modules must not depend on feature modules.
- Feature impl modules may depend on feature API modules.
- Feature API modules must not depend on other features.
- App may depend on all modules as the composition root.
- Avoid circular dependencies. Flag any new dependency that introduces or suggests a cycle.

### Repository ownership
- Repository interface lives in `:core:domain/repository/<Name>Repository.kt`
- Repository implementation lives in `:data:network/repository/Supabase<Name>Repository.kt`
- DI binding lives in `:app/di/RepositoryModule.kt`
- Use `@Provides` for repository bindings. Do not assume KMP implementations are Hilt-constructible with `@Inject`.

### File naming
- Screens: `*Screen.kt`
- ViewModels: `*ViewModel.kt`, annotated `@HiltViewModel`
- Routes: `@Serializable` route classes in `:feature:<name>:api/Routes.kt`
- Navigation: `*Navigation.kt` with `NavGraphBuilder.<feature>Graph(navController)`
- Repository interfaces: `<Name>Repository.kt`
- Repository implementations: `Supabase<Name>Repository.kt`
- API classes: `*Api.kt`
- DTOs: `*Dto.kt`
- Mappers: `*Mapper.kt`

## Additional rules for this project

### Clean architecture / separation of concerns
- UI modules must not own networking, DTO mapping, or persistence logic.
- Business rules should live in `:core:domain` unless they are strictly UI-only.
- Mapping logic must not live in screens or composables.
- Avoid god classes, especially oversized ViewModels with mixed responsibilities.
- New code should live in the module that matches its responsibility, not where it was easiest to add.

### MVI / unidirectional data flow
- Each screen/ViewModel should expose a clear UI state model.
- User actions should be modeled as events/intents, not ad-hoc mutable flags spread across the UI.
- One-off effects should be separated from persistent UI state.
- Avoid duplicate mutable state sources for the same screen.
- Composables should render state and emit actions, not make business decisions.

### Data flow expectations
- UI actions/events flow inward from UI to ViewModel/domain.
- State/data flows outward to UI via reactive streams.
- Avoid bidirectional dependencies between layers.
- Prefer Kotlin Flow for reactive state and data streams.
- Review ViewModel state management for consistency and testability.

### Coroutines / structured concurrency
- ViewModels must launch coroutines from `viewModelScope`.
- Do not use `GlobalScope` or unmanaged `CoroutineScope()`.
- Concurrent work should preserve structured concurrency (`coroutineScope` / `supervisorScope` when justified).
- Do not swallow `CancellationException`.
- Error handling must be explicit and map to state/effects/logging as appropriate.
- Avoid blocking calls hidden inside suspend/coroutine code.

### Navigation boundaries
- Feature API modules own route/navigation contracts only.
- Feature impl modules own graph wiring and screen registration.
- Cross-feature communication must go through feature API contracts.
- Avoid direct feature impl → feature impl dependencies.

### Strings and platform boundaries
- No user-facing strings hardcoded in Kotlin; use `strings.xml`.
- No Android imports in KMP `commonMain`.
- No DTOs or Ktor types outside `:data:network`.
- No shared Compose UI across platforms.

### Explicit project constraints
- No Supabase SDK — only Ktor + REST.
- No client-side rating calculation — rating changes are server-side only via Edge Functions.
- No feature impl → feature impl dependencies.
- No `@Binds` for repository bindings in this project.

## Procedure

1. Enumerate touched files (with line ranges if diff-based).
2. For each file, check:
   - Does its location match its responsibility?
   - Do imports respect the allowed dependency graph?
   - Does the module/build file declare only allowed dependencies?
   - Is any Android API leaking into KMP `commonMain`?
   - Is the state/event handling aligned with MVI expectations?
   - Are coroutine launches, cancellation, and error handling structured correctly?
3. Run cross-cutting checks:
   - Any new `implementation(projects.feature.x.impl)` from another feature? → Block
   - Any business logic duplicated in ViewModels or UI that belongs in `:core:domain`? → Smell or Block depending on severity
   - Any DTO / Ktor import outside `:data:network`? → Block
   - Any hardcoded user-facing strings in Kotlin? → Smell
   - Any missing `strings.xml` additions for new UI copy? → Smell
   - Any unmanaged coroutine scope / swallowed cancellation / silent failure path? → Smell or Block depending on severity
   - Any circular dependency introduced or implied by new module wiring? → Block
4. Classify findings as:
   - **Block** — clear violation, fix before merge
   - **Smell** — likely wrong or risky, needs justification or follow-up
   - **Nit** — naming/style/minor consistency issue

## Output format

```text
### Architecture review — <scope>

**Blockers**
- `path/to/File.kt:LL` — <what + why it violates the rule>

**Smells**
- ...

**Nits**
- ...

**Clean**
- <short mention of what is correctly placed, so user sees the positive signal>
```

Keep each bullet ≤2 lines. If there are zero blockers and zero smells, lead with "LGTM" and list only nits (if any).

## Boundaries for this skill

- Do NOT suggest refactors outside the scope being reviewed.
- Do NOT rewrite code in this skill — only flag. Hand off to `feature-scaffold` or a normal edit session for fixes.
- Do NOT comment on product/UX choices. Architecture only.