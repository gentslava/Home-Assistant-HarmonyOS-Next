# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context (single source of truth)

@AGENTS.md

Everything operational — what the app is, build/run/test/lint, layout, strict-ArkTS code style,
and gotchas — lives in `AGENTS.md` above so it is shared with every AI tool. **Read it first.**
Do not duplicate that content here; this file holds only Claude Code-specific guidance.

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
  - `arkts-architect` — planning a feature, a layer/contract change, or a new HA domain.
  - `code-reviewer` — review any non-trivial change in a fresh context before commit.
  - `test-writer` — add/extend hypium tests under `entry/src/test` or `entry/src/ohosTest` (in `apps/watch-arkts/`).
- **Verify, don't assume.** A task is done only when the emulator still runs in Mock mode and the
  linter reports no new ArkTS errors/warnings. State the evidence; don't claim success without it.
  This repo has no committed `hvigorw`/CI, so verification is mostly via DevEco Studio — if you
  cannot run a check, say so explicitly instead of asserting it passed.
- **Scope discipline.** Touch only what the task needs. Don't remove the commented `setRemoteApp`
  call or the `module.json5` placeholders — they are deliberate (see gotchas in `AGENTS.md`).
- **Surface assumptions and ask** when requirements are ambiguous — the repo author prefers an
  extra question over a wrong inference.

## Maintaining this file

Treat `CLAUDE.md` and `AGENTS.md` like code: prune them when something stops being true. If a rule
here is being ignored, the file is probably too long — tighten it. Keep instructions in `AGENTS.md`;
keep this file a thin Claude-specific layer.
