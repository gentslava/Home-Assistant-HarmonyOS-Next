# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context (single source of truth)

@AGENTS.md

Operational context lives in `AGENTS.md` (a router) and the **per-app `apps/<app>/AGENTS.md`** for
each platform. **Read the root one, then the per-app one for the app you're touching** — applying
ArkTS rules to the Kotlin or lite-JS app is the most common mistake here. This file holds only
Claude-specific guidance.

## Deep-dive references (load on demand)

- [docs/architecture.md](docs/architecture.md) — system map, layers, startup & data flow.
- [docs/p2p-protocol.md](docs/p2p-protocol.md) — watch↔phone wire contract (companion is a separate, unwritten repo).
- [docs/adr/](docs/adr/) — why decisions were made; **don't re-litigate an Accepted ADR** — supersede it with a new one.
- [DEVELOPMENT.md](DEVELOPMENT.md) — the AI-first workflow and review/test agent roles.
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution rules, PR checklist, "add a new HA domain".

## How to work here

- **Explore → Plan → Code → Commit.** For anything beyond a one-line diff, plan before editing
  (use plan mode); skip the plan only when the change is describable in one sentence. Details in
  [DEVELOPMENT.md](DEVELOPMENT.md).
- **Use the role subagents** in `.claude/agents/` rather than doing everything inline:
  - `architect` — planning a feature, a protocol/contract change, or a new HA domain across apps.
  - `code-reviewer` — review any non-trivial change in a fresh context before commit (applies the file's per-app rules).
  - `test-writer` — add/extend tests with the right framework per app (hypium / JUnit / on-device).
- **Verify, don't assume.** Verification is per-app (see each `AGENTS.md`): watch-arkts = emulator
  Mock run + Code Linter; watch-lite = builds/installs on a real GT; companion = `./gradlew test`.
  State the evidence; if you couldn't run a check, say so instead of asserting it passed.
- **Scope discipline.** Touch only what the task needs, and don't carry one app's conventions into
  another. `setRemoteApp` / `PEER_FINGERPRINT` are now filled with the real companion bundle +
  debug-cert fingerprint; the remaining deliberate placeholder is `module.json5`'s `client_id`
  (`PUT_YOUR_CLIENT_ID_HERE`) — don't remove it. See the per-app gotchas.
- **Surface assumptions and ask** when requirements are ambiguous — the repo author prefers an
  extra question over a wrong inference.

## Maintaining this file

Treat `CLAUDE.md` and `AGENTS.md` like code: prune them when something stops being true. If a rule
here is being ignored, the file is probably too long — tighten it. Keep instructions in `AGENTS.md`;
keep this file a thin Claude-specific layer.
