---
name: feature-scaffold
description: Scaffold a new screen, flow, or full feature module for Squash & Go following the project's exact conventions — api/impl split, serializable route, HiltViewModel with sealed UiState + one-shot effects, repository wiring, strings.xml, nav graph entry, structured-concurrency error handling. Use when the user says "add a screen", "scaffold feature X", "new screen for Y", or asks to stub a flow.
---

# feature-scaffold

Use this skill when the user asks to create a new screen, new flow, or new feature module. Follow conventions exactly — do not improvise structure.

## Before writing any code

Ask only the questions you can't answer from context:
1. **Where does this belong?** Which feature module? If none fits, do we create a new `:feature:<name>:*` pair?
2. **What data does it need?** Existing repository? New repository? New endpoint?
3. **What's the entry point?** Deep link? Tab? Nav from another screen?
4. **What does success look like?** Primary action + secondary + empty/error states.

If the answers are clear from the task, skip the questions and state your assumptions in one line before scaffolding.

## Scaffold — single screen in existing feature

For a screen called `Foo` inside `:feature:<name>`:

### 1. Route — `:feature:<name>:api/Routes.kt`
```kotlin
@Serializable
object FooRoute
// OR, for a screen that needs args:
@Serializable
data class FooRoute(val someId: String)
```

### 2. Screen — `:feature:<name>:impl/.../FooScreen.kt`
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FooScreen(
    onBack: () -> Unit,
    viewModel: FooViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Scaffold + when(state) over sealed UiState
}
```
All user-facing strings via `stringResource(R.string.foo_*)`. Never inline.

Composables render state and emit actions — they must not make business decisions, not launch coroutines, not call repositories directly.

### 3. ViewModel — `:feature:<name>:impl/.../FooViewModel.kt`
```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(
    private val repo: SomeRepository,
    // SessionManager if the screen needs the current user id
) : ViewModel() {
    private val _uiState = MutableStateFlow<FooUiState>(FooUiState.Loading)
    val uiState: StateFlow<FooUiState> = _uiState

    // One-shot effects (navigation, toasts) go through a separate channel/SharedFlow,
    // never mixed into UiState.
    private val _effects = Channel<FooEffect>(Channel.BUFFERED)
    val effects: Flow<FooEffect> = _effects.receiveAsFlow()

    // load() in init or as an explicit method called from LaunchedEffect
}

sealed interface FooUiState {
    data object Loading : FooUiState
    data class Ready(/* ... */) : FooUiState
    data object Error : FooUiState
}

sealed interface FooEffect {
    data object NavigateBack : FooEffect
    data class ShowMessage(val messageRes: Int) : FooEffect
}
```

**Rules for the VM:**
- Launch coroutines from `viewModelScope` only. No `GlobalScope`, no `CoroutineScope()`.
- Every `launch` body is wrapped in try/catch that rethrows `CancellationException` first, then logs the `Throwable` with `Log.e(TAG, ...)`, then emits an `Error` state (or a typed error state).
- Never surface a raw exception message to the UI. Map to string resource ids.
- User actions = methods on the VM. Do not expose mutable state externally.
- Do not duplicate the same mutable state across multiple flows/fields.

### 4. Strings — `:feature:<name>:impl/src/main/res/values/strings.xml`
Add keys with pattern `<feature>_<screen>_<purpose>`:
```xml
<string name="foo_title">…</string>
<string name="foo_empty">…</string>
<string name="foo_error_generic">…</string>
```
If the module has no `res/values/strings.xml` yet, create the directory and file. Use `<plurals>` for counts. No concatenated sentences in Kotlin.

### 5. Navigation — `:feature:<name>:impl/.../navigation/<Name>Navigation.kt`
Inside `NavGraphBuilder.<name>Graph(navController)` add:
```kotlin
composable<FooRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<FooRoute>() // only if route has args
    FooScreen(
        onBack = { navController.popBackStack() },
        // wire other nav callbacks — only use routes from other features' :api modules
    )
}
```
Cross-feature navigation must reference other features' `:api` modules only. Never import another feature's `:impl`.

### 6. If a new repository is needed
- Interface in `:core:domain/repository/<Name>Repository.kt` (KMP commonMain).
- Impl in `:data:network/repository/Supabase<Name>Repository.kt` (KMP commonMain). Uses Ktor + `*Api` class. No Android imports.
- `*Api.kt` in `:data:network/api/` with PostgREST calls.
- DTOs in `:data:network/dto/` (`@Serializable`, `@SerialName` for snake_case fields).
- Mapper in `:data:network/mapper/` as `object <Name>Mapper { fun toDomain(dto): Domain }`.
- DI: add `@Provides` in `:app/di/ApiModule.kt` (for Api) and `:app/di/RepositoryModule.kt` (for Repo). Never `@Binds` for KMP impls.

## Scaffold — new feature module pair

For `:feature:newthing`:

1. `settings.gradle.kts`: `include(":feature:newthing:api")` and `include(":feature:newthing:impl")`.
2. `feature/newthing/api/build.gradle.kts`: apply `squashgo.android.library` + `org.jetbrains.kotlin.plugin.serialization`. Namespace `com.egr.squashgo.feature.newthing.api`.
3. `feature/newthing/impl/build.gradle.kts`: apply `squashgo.android.feature`. Depend on `projects.feature.newthing.api` + needed core modules.
4. Add `Routes.kt` in `:api` and start/home screen in `:impl`.
5. Create `newthingGraph(navController)` in `<Name>Navigation.kt`.
6. Add `implementation(projects.feature.newthing.impl)` in `:app/build.gradle.kts`.
7. Call `newthingGraph(navController)` in `SquashGoApp.kt`'s NavHost.
8. Create `res/values/strings.xml` in `:impl`.

## Hard rules (never violate)

- `:feature:<a>:impl` must **not** depend on `:feature:<b>:impl`. Use the other feature's `:api` only.
- No Android imports in `:core:domain` or `:core:model`.
- No Ktor imports outside `:data:network`.
- No `@Inject constructor` on KMP classes — use `@Provides` in app DI.
- No hardcoded user-facing strings in Kotlin — always `stringResource` + `strings.xml`.
- Rating changes happen server-side in Edge Functions, never in a ViewModel.
- No Supabase SDK. Ktor + REST only.
- Every `viewModelScope.launch { … }` catches and rethrows `CancellationException`, then logs + emits typed error state.

## Output

When finished, write a short summary:
- Files created/modified with one-line purpose each.
- What still needs to happen outside code (migration, DI wiring if skipped, manual Supabase config).
- Next step the user should take (e.g., "Sync Gradle, then open FooScreen via …").
