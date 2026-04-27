# White-Label Architecture

## Goal

Squash & Go is designed to be deliverable as a **white-label product**: the same codebase, packaged and branded as multiple distinct apps for different clients (or markets). A new client should be onboarded by:

1. Creating a new `:app` module (or repo, eventually).
2. Providing client-specific configuration (Supabase project, branding, feature set, copy).
3. Compiling and shipping.

No client-specific logic should leak into shared modules. `:app` is the **per-client customization surface** — even when there is only one client today.

## Why not Android product flavors

Flavors solve "different applicationId / different colors / different strings in same module". They do **not** solve:
- Independent gradle dependencies per client.
- Independent feature sets where features can be omitted entirely.
- Independent client repos (the eventual model — shared modules published to Maven, consumed per-client).
- Cross-platform parity (iOS has no notion of Android flavors).

Flavors may still appear later as a development convenience (e.g. `dev` vs `prod` BuildConfig values inside one client `:app`), but they are not the primary white-label mechanism.

## Module layout

```
:core:config                (KMP)               — shared config contracts (data classes)
:core:config:android        (Android + Hilt)    — Hilt module + ConfigBootstrap registry
:data:network               (KMP)               — Ktor + APIs + repository impls (config-agnostic)
:data:network:di            (Android + Hilt)    — Hilt wiring of network for Android
:feature:*                  (Android)           — features, consume config via Hilt
:app  (per-client)          (Android app)       — Application + MainActivity + branding + feature graph composition
```

### What lives where (the rule of thumb)

| Concern | Module | Notes |
|---|---|---|
| Config data classes (`SupabaseConfig`, `BrandingConfig`, `FeatureSet`) | `:core:config` (KMP) | Pure data, shared with iOS |
| Hilt provision of config | `:core:config:android` | One-time, all client `:app`s share it |
| Network DI (Ktor, APIs, repos) | `:data:network:di` | Consumes `SupabaseConfig` via Hilt |
| Per-client Application + bootstrap call | client `:app` | Reads `BuildConfig`, calls `ConfigBootstrap.configure(...)` |
| Per-client resources (strings, colors, logo) | client `:app/res/` | Standard Android override |
| Per-client feature graph composition | client `:app/SquashGoApp.kt` | Declares which `:feature:*:impl` graphs to include |

## The bootstrap registry pattern

The challenge: every client `:app` needs to provide config to Hilt. We want to avoid duplicating the `@Module` boilerplate per client. Solution: **declare the module once in shared code, and have it read from a registry that each `:app` initializes in `Application.onCreate()`**.

### Shared module — `:core:config:android`

```kotlin
object ConfigBootstrap {
    private var supabaseConfig: SupabaseConfig? = null

    fun configure(supabaseConfig: SupabaseConfig) {
        this.supabaseConfig = supabaseConfig
    }

    internal fun requireSupabaseConfig(): SupabaseConfig =
        supabaseConfig
            ?: error("ConfigBootstrap.configure() must be called in Application.onCreate()")
}

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = ConfigBootstrap.requireSupabaseConfig()
}
```

### Per-client `:app`

```kotlin
@HiltAndroidApp
class ClientASquashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ConfigBootstrap.configure(
            SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
            ),
        )
    }
}
```

That is the entire per-client wiring. No `@Module`, no `@Provides`, no Hilt annotations to repeat. Adding `BrandingConfig`, `FeatureSet`, etc. only adds one line to the bootstrap call.

### Order-of-operations note

`ConfigBootstrap.configure(...)` must run **before** any Hilt injection that requests `SupabaseConfig`. Calling it in `Application.onCreate()` is safe because Hilt's component is built lazily on first `@AndroidEntryPoint` injection (typically when `MainActivity` is created — strictly after `Application.onCreate()` completes).

### Tradeoff

Mutable global state. If `configure()` is forgotten, the app crashes at first injection with a clear error. The alternative (a Gradle convention plugin that generates a `@Module` per `:app`) avoids global state but adds build-system complexity. The registry is simpler and the failure mode is loud.

## iOS parity

The pattern works on both platforms because **the contract lives in KMP `:core:config`** and the **provisioning is platform-native**.

| Layer | Android | iOS |
|---|---|---|
| `SupabaseConfig` data class | `:core:config` (commonMain) | same — consumed via XCFramework |
| Provision mechanism | `:core:config:android` `ConfigBootstrap` + Hilt module | Native Swift DI (initializer injection / Swinject / manual factory) |
| Where config is constructed | `Application.onCreate()` of client `:app` | `App.swift` `init()` of client iOS app target |
| Where it's consumed | `:data:network:di` Hilt module → `SupabaseApi.createClient(...)` | Swift code constructs Ktor client through KMP `SupabaseApi.createClient(...)` |

### Why we do NOT make `ConfigBootstrap` itself KMP

Two reasons:
1. **Hilt is Android-only.** The bootstrap registry on Android exists specifically to feed Hilt. iOS doesn't need it — Swift idiomatic DI constructs and passes config directly through initializers, not through a global registry.
2. **iOS has its own white-label mechanism**: separate Xcode targets / schemes per client (analogous to Android multi-`:app`). Each iOS client target constructs its own `SupabaseConfig` and wires it natively.

### What KMP modules MUST guarantee for iOS parity

- Pure config: no reading of any `BuildConfig`, `Bundle`, `Context`, or platform-specific source from inside KMP modules.
- All KMP entry points that need config accept it as **parameters or constructor args**, never via implicit globals.
  - `SupabaseApi.createClient(config: SupabaseConfig, ...)` — both platforms construct config and pass it in.
  - Repository implementations take their dependencies via constructor.
- No Hilt annotations or Android imports anywhere in commonMain.

This is the existing convention; the white-label pattern just makes it explicit.

## Implementation order (for the current refactor)

1. Create `:core:config` (KMP) with `SupabaseConfig` data class. Move it out of `:data:network/config/`.
2. Create `:core:config:android` (Android + Hilt) with `ConfigBootstrap` + `AppConfigModule`.
3. Delete `:app/di/SupabaseConfigModule.kt`. In `SquashGoApplication.onCreate()`, call `ConfigBootstrap.configure(...)`.
4. Verify build, commit.
5. Separately: shell refactor (move bootstrap VM/state into `:feature:shell:impl`).

Future work, in rough order of likely arrival:
- `BrandingConfig` (theme tokens, logo) consumed by `:core:designsystem`.
- `FeatureSet` (which features are available) consumed by `:app/SquashGoApp.kt` to compose the nav graph.
- A `ClientConfigSource` interface so `BrandingConfig` can come from `BuildConfig` today and from a CMS API tomorrow without changing downstream consumers.
- 2nd client `:app` module to validate the pattern.
- Maven publishing of shared modules + per-client repos (the "real" white-label setup mirroring Erik's work).

## Anti-patterns to avoid

- ❌ Reading `BuildConfig.SUPABASE_URL` from any module other than the per-client `:app`.
- ❌ Hardcoding brand colors / strings / endpoints in shared modules.
- ❌ Adding `@Provides` for client-specific values inside `:feature:*` or `:core:*` modules.
- ❌ Using `@UninstallModules` + replacement modules as a routine override mechanism (only for tests).
- ❌ Treating `:app` as if it must be empty. It is not — it is **the contract** between a client and the shared codebase.
