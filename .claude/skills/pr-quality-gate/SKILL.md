---
name: pr-quality-gate
description: Pre-merge review for a Squash & Go branch or PR — architecture, feature correctness, tests, strings extraction, logging, Supabase migration/RLS safety, backward compatibility, and ship/no-ship verdict. Use when the user says "review this PR", "ready to merge?", or "pre-merge check".
---

# pr-quality-gate

Use this skill when the user asks for a merge-readiness check, PR review, or ship/no-ship verdict. This is the final gate before `main`.

## Scope resolution

- If there's an open PR: use `gh pr view` + `gh pr diff` for the numbered PR.
- If on a branch not yet pushed: diff current branch vs `main` — `git diff main...HEAD` and `git log main..HEAD`.
- If the user points to specific files/commits: honor that.

Ask only when ambiguous.

## Checklist (run all, in order)

### 1. Architecture (reuse `architecture-review` rules)
- Module placement correct? (screens in `:feature:*:impl`, repos split across `:core:domain` + `:data:network`, routes in `:feature:*:api`.)
- No `:feature:<a>:impl` → `:feature:<b>:impl` dep.
- No Android imports in KMP commonMain.
- No Supabase SDK. Ktor only.
- DI uses `@Provides`, not `@Binds`, for KMP impls.
- No circular module deps introduced.

### 2. Feature correctness
- Does the code match the described intent? Read the PR title/description (or recent commit messages) and verify the diff actually delivers it.
- Edge cases handled: loading, empty, error, offline, expired session (401).
- Every `viewModelScope.launch` catch block rethrows `CancellationException` before mapping to error state.
- No rating calculation on the client — server-side via Edge Function only.

### 3. Strings & UX copy
- Every user-facing string (titles, buttons, empty, error, placeholder, content description) in `strings.xml` — not hardcoded in Kotlin.
- Keys follow `<feature>_<screen>_<purpose>` pattern.
- Pluralized counts use `<plurals>` + `pluralStringResource`, not string concat.
- Error messages are human ("We couldn't load…"), not raw exception messages.

### 4. Tests
- Domain logic touched? → unit tests in `core/domain/src/commonTest/`. (See `unit-test-coverage` skill rules.)
- ViewModel touched? → StateFlow test with Turbine + `runTest` + StandardTestDispatcher.
- New repository impl? → MockEngine test covering happy path + at least one 4xx/5xx.
- Elo / Tier / Score / Challenge-state / Match-state changes **must** have tests. No exceptions.

### 5. Supabase migrations (`supabase/migrations/*.sql`)
- RLS enabled on every new table (`ALTER TABLE x ENABLE ROW LEVEL SECURITY;`)?
- Policies cover SELECT/INSERT/UPDATE/DELETE as appropriate for the table's ownership model?
- Forward-only — no `DROP` of existing user data, no destructive `ALTER` without a backfill plan?
- Idempotent where it should be — seeds use `NOT EXISTS` or `ON CONFLICT DO NOTHING`; migrations use `CREATE … IF NOT EXISTS` where applicable?
- Numbered correctly — next sequential number after existing files?
- Trigger/function changes handle existing rows?

### 6. Logging & observability
- No `println` in production code. Use `Log.e(TAG, msg, throwable)` with a const TAG.
- No sensitive values in logs (access tokens, emails, full JWTs).
- Errors that reach the UI are logged with enough context to debug from Logcat.

### 7. Security
- No secrets committed (service role key, FCM keys, API URLs should come from BuildConfig or environment, not hardcoded strings).
- RLS is the only auth layer for DB access — no "trust the client" queries.
- Input validation at boundaries: display names trimmed + length-checked, court IDs validated as UUIDs before network calls.

### 8. Backward compatibility
- Any API/contract change (DTO field rename, Route `@Serializable` shape, public ViewModel state) — is there a consumer that breaks? For an MVP with one client this is usually fine, but call it out explicitly.
- Migrations that rename a column: did the DTO's `@SerialName` update too?

### 9. Dead code & cleanup
- No unused imports, no commented-out blocks, no `// TODO(erik)` left without a tracking note.
- No debug flags flipped on (e.g., Ktor `LogLevel.ALL` in a release config).

## Output format

```text
## Pre-merge review — <branch or PR>

### Verdict: ✅ SHIP  |  ⚠️ SHIP AFTER FIXES  |  ❌ HOLD

### Must-fix
- `path/File.kt:LL` — <issue + suggested fix in one line>

### Should-fix
- …

### Nice-to-have
- …

### Risk notes
- <anything that could bite post-merge — e.g. "migration 003 must be applied to prod before this deploy" or "no push token cleanup on sign-out yet">

### Tests run
- `<module>:test` — passed / failed / not run (why)
```

Emojis are allowed in the verdict line only (they signal at-a-glance status). Nowhere else.

## Rules for this skill

- Don't write fixes — this is a gate, not a remediation session. Hand off to `feature-scaffold` or a normal edit session for fixes.
- Don't approve (`✅ SHIP`) if a Must-fix exists. Downgrade to `⚠️` or `❌`.
- Always run the tests if the environment allows. A passing test suite is part of the gate.
- If the PR has zero tests and the diff touches domain logic or state machines, that's an automatic `❌ HOLD` regardless of everything else.
