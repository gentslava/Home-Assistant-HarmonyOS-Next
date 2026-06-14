# 0007. Per-app AGENTS.md for the multi-platform monorepo

- **Status:** Accepted
- **Date:** 2026-06-15

## Context

After [ADR-0006](0006-monorepo-structure.md) the repo holds **three platforms** with incompatible
rules: `watch-arkts` (strict ArkTS, ArkUI V2, hypium), `watch-lite` (JS limited to **ES5.1** —
no Promise/globalThis/spread, callbacks), `phone-android` (Kotlin, Compose, Gradle/JUnit). The AIDD
layer was written for the single ArkTS app, so the root `AGENTS.md` and the subagents carried
ArkTS-only code style and tooling. An agent editing the Kotlin or lite-JS app would read ArkTS rules
and apply them — exactly the "stale/contradictory instructions" failure AIDD warns about, and the
risk grew with every new cross-platform file.

## Decision

Use **nested `AGENTS.md`** (the agents.md standard: an agent reads the nearest one to the file it
edits):

- `apps/<app>/AGENTS.md` — platform-specific rules (code style, build/test, gotchas) for each app.
- Root `AGENTS.md` — a **router**: shared context (monorepo, P2P contract, workflow, scope
  discipline) + a table mapping each app to its `AGENTS.md`. ArkTS specifics moved down to
  `apps/watch-arkts/AGENTS.md`.
- Subagents (`.claude/agents/`) are **platform-aware**: `architect` (was `arkts-architect`) plans
  across all three; `code-reviewer` and `test-writer` read the file's per-app `AGENTS.md` and apply
  that platform's rules / framework.

## Consequences

- An agent gets the right rules for the file it's touching; the most common mistake (ArkTS rules on
  Kotlin/JS) is structurally prevented.
- Three more files to keep current — but each is small and local; the root file shrank.
- New platforms slot in by adding `apps/<app>/AGENTS.md` + a router row.
- Reviewers/test-writers must consult the per-app file; the subagents now say so explicitly.

## Alternatives considered

- **One root AGENTS.md with per-platform sections** — rejected: the root keeps growing and an agent
  still has to find the right section; nested files put rules next to the code they govern.
- **Separate repos per platform** — rejected in [ADR-0006](0006-monorepo-structure.md) (contract drift).
