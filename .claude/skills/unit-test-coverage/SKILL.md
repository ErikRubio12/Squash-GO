---
name: unit-test-coverage
description: Review a Squash & Go change for unit test coverage and write targeted tests — pure domain logic (Elo, tier, score validation, state machines), ViewModels (StateFlow + coroutines with Turbine/runTest), repository impls with Ktor MockEngine, DTO↔domain mappers. Use when the user asks for tests, coverage, or "what should I test here".
---

# unit-test-coverage

Use when the user asks for tests, says "add coverage", or asks whether code is worth testing.

## What to test — priority order

1. **Pure domain logic** (`:core:domain`) — always test. `EloCalculator`, `TierMapper`, `ScoreValidator`, challenge/match state transitions. Fast, deterministic, no mocks needed. Every branch + boundary values (1199 vs 1200 tier edge, deuce at 10-10, provisional→established at 5 matches).
2. **ViewModels** (`:feature:*:impl`) — state transitions and side-effect sequencing, not Compose rendering. Use a fake repository (handwritten, not Mockito) + `runTest` + Turbine `.test { }`.
3. **Repository impls** (`:data:network`) — DTO↔domain mapping, query-param shape, error-path branching. Use Ktor `MockEngine` to stub responses.
4. **Mappers** (`:data:network`) — only if non-trivial (nested joins, nullable fields, polymorphic shapes). Skip for pure field-copy mappers.

## What NOT to test

- Compose screens (no UI test infra in MVP — validated manually on device).
- Pure data classes (no logic).
- DI modules.
- Trivial `toDomain` mappers that are 1:1 field copies — one spot-check is enough.
- Supabase migrations (validated by applying them).

## Conventions

### Test module placement
- Pure domain: `core/domain/src/commonTest/kotlin/…` (KMP). Framework: `kotlin-test`.
- ViewModels: `feature/<name>/impl/src/test/kotlin/…` (Android unit test). Framework: JUnit4 + Turbine + `kotlinx-coroutines-test`.
- Repos & Mappers: `data/network/src/commonTest/kotlin/…` (KMP). Framework: `kotlin-test` + Ktor MockEngine.

### Naming
- Class: `<SubjectUnderTest>Test.kt` (e.g., `EloCalculatorTest.kt`, `LoginViewModelTest.kt`).
- Methods: backtick sentences — `given X when Y then Z`:
```kotlin
@Test
fun `given provisional player when ranked match played then k-factor is 60`() { … }
```
- Group related tests with section comments: `// region Score validation` / `// endregion`.

### ViewModel test pattern
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeFooRepository()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `given success response when load then emits Ready`() = runTest(dispatcher) {
        fakeRepo.nextResult = Result.success(someDomain)
        val vm = FooViewModel(fakeRepo)
        vm.uiState.test {
            assertIs<FooUiState.Loading>(awaitItem())
            assertIs<FooUiState.Ready>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Fakes over mocks
Prefer handwritten fakes:
```kotlin
class FakeFooRepository : FooRepository {
    var nextResult: Result<Foo> = Result.failure(IllegalStateException("not set"))
    override suspend fun getFoo(): Foo = nextResult.getOrThrow()
}
```
Mocking libraries are heavyweight and brittle; fakes read clearly and double as executable contract docs. Only reach for MockK/Mockito when the interface has >10 methods or needs call-order verification.

### Coroutine hygiene
- Always use `runTest(dispatcher)` with an injected `StandardTestDispatcher`.
- Set `Dispatchers.setMain` in `@Before`, reset in `@After`. Never in the test body.
- Use Turbine's `.test { }` for StateFlow — do not assert on `.value` directly; it's a race.
- Verify that `CancellationException` is rethrown (don't just assert the happy path — structured concurrency is a correctness property).

### Ktor MockEngine pattern (repo tests)
```kotlin
val engine = MockEngine { request ->
    assertEquals("/rest/v1/players", request.url.encodedPath)
    assertEquals("eq.uuid-1", request.url.parameters["id"])
    respond(
        content = """[{"id":"uuid-1","display_name":"Erik", …}]""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}
val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
```
Assert on request shape (path, query params, body) + response parsing + error-path mapping.

## Required coverage for MVP-critical logic

These are load-bearing for the product and must have tests:

1. **`EloCalculator`** — K-factor selection (60/40/32), score formula math, rating update symmetry.
2. **`TierMapper`** — every tier boundary (800, 1000, 1200, 1400, 1600, 1800), every division boundary within a tier, provisional → "Calibrating" label, clamping below 800.
3. **`ScoreValidator`** — best-of-5 winner detection, 11+ points win-by-2 deuce, invalid scores (14-10 with no deuce, negative, missing games).
4. **Challenge state machine** — each valid transition (`PENDING → ACCEPTED/DECLINED/CANCELLED/EXPIRED → COMPLETED`) + each invalid transition rejected.
5. **Match state machine** — `IN_PROGRESS → RESULT_SUBMITTED → CONFIRMED/DISPUTED`, plus 72h auto-confirm trigger predicate.

Flag any of these that don't yet have tests as "critical gap".

## Procedure when invoked

1. Determine scope (files changed, feature named, or whole `:core:domain`).
2. List what exists vs what's missing. Classify: **Critical gap** / **Recommended** / **Optional** / **Not worth testing**.
3. For each gap the user approves (or all critical gaps by default), write the test file following the conventions above.
4. Run `./gradlew :<module>:test` (or `:<module>:testDebugUnitTest` for Android) if the environment supports it — report pass/fail.

## Output format

```text
### Test review — <scope>

**Critical gaps** (write these now)
- `<module>.<Class>.<method>` — <why this matters>

**Recommended**
- …

**Optional**
- …

**Not worth testing** (and why)
- …
```

Ask before writing unless the user said "write tests" outright. Keep each generated test file focused (one class under test).
