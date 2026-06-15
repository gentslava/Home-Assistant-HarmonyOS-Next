# AGENTS.md

Single source of truth for AI coding agents (Claude Code, Cursor, Copilot, Codex, Gemini, …) in this
repository. Plain Markdown per the [agents.md](https://agents.md/) standard. `CLAUDE.md` imports this
file; do not duplicate content there.

This is a **monorepo with three platforms**, so rules are split: this file holds what's shared, and
each app has its **own `AGENTS.md`** with platform-specific rules (agents read the nearest one to the
file they're editing). **Always read the per-app AGENTS.md for the app you're touching** — applying
ArkTS rules to the Kotlin or lite-JS app is the most common mistake here.

Keep every file lean: only what an agent can't infer from code, would get wrong by default, or how
this repo expects work to happen. Prune what stops being true.

## What this is

A [Home Assistant](https://www.home-assistant.io/) client for Huawei wearables, plus its phone
companion. The watch has **no direct network path to HA** — it talks to an Android **companion** over
**Wear Engine P2P**, and the companion holds the HA REST connection. Huawei splits watches into two
incompatible developer platforms, so there are two watch apps sharing one design and one wire
contract ([docs/platform-constraints.md](docs/platform-constraints.md)).

Maturity: real-HA-integration in progress ([docs/specs/01-real-ha-integration.md](docs/specs/01-real-ha-integration.md)).
Domains: `light`, `switch`, `lock`, `cover`, `scene`, `sensor` (sensor read-only). Localization: EN + RU.

## The three apps — read the right rules

| App | Platform | Per-app rules | One-line |
|-----|----------|---------------|----------|
| [`apps/watch-arkts/`](apps/watch-arkts/AGENTS.md) | ArkTS + ArkUI (Stage) | [AGENTS.md](apps/watch-arkts/AGENTS.md) | Full wearable (Watch 4/5/Ultimate); strict ArkTS, V2 state, hypium |
| [`apps/watch-lite/`](apps/watch-lite/AGENTS.md) | JS (FA, JerryScript) | [AGENTS.md](apps/watch-lite/AGENTS.md) | Watch GT; **limited JS** (no Promise/globalThis/spread), HML/CSS/JS, callbacks |
| [`apps/phone-android/`](apps/phone-android/AGENTS.md) | Kotlin + Compose | [AGENTS.md](apps/phone-android/AGENTS.md) | Companion: P2P↔HA REST; Gradle/JUnit, OkHttp, HMS Wear Engine |

Each app opens as its **own project root** in its IDE (watches → DevEco Studio, companion → Android
Studio); the git root is the monorepo root.

The two watch apps are **mirror architectures in different runtimes** — they share **design**, not
code: the [P2P contract](docs/p2p-protocol.md), the domain model, the layering. ArkTS compiles to
`PANDA` bytecode, lite-JS to a haptobin `.bin` — different engines, no shared binary.

## Shared knowledge

| | |
|---|---|
| Wire contract (watch ↔ phone) | [docs/p2p-protocol.md](docs/p2p-protocol.md) — `v:1`, correlate by `id`, 8s timeout |
| HA API + HA→P2P mapping | [docs/ha-integration-notes.md](docs/ha-integration-notes.md) |
| watch-arkts architecture | [docs/architecture.md](docs/architecture.md) |
| Why Watch, not GT | [docs/platform-constraints.md](docs/platform-constraints.md) |
| Decisions (don't re-litigate) | [docs/adr/](docs/adr/) |
| MVP spec | [docs/specs/01-real-ha-integration.md](docs/specs/01-real-ha-integration.md) |

## How work happens here (all apps)

- **Explore → Plan → Code → Commit.** Plan before non-trivial edits. Process & agent roles:
  [DEVELOPMENT.md](DEVELOPMENT.md).
- **Bundle id** `ru.gentslava.homeassistant` (watches) / `…​.companion` (phone) is shared identity;
  the P2P `supportLists`/`setRemoteApp`/`setPeerPkgName` pairing ties the three together.
- **Protocol changes are breaking:** bump `v` and update [docs/p2p-protocol.md](docs/p2p-protocol.md)
  **and every app** that speaks it. Don't change one side only.
- **Keep the mock path working** in each watch app as an offline/dev fallback.
- **Commits:** Conventional Commits (`feat:`/`fix:`/`docs:`/…), one focused change per PR. Scope-tag
  by app when useful (`feat(companion): …`, `feat(watch-lite): …`).
- **Secrets never committed:** HA tokens, `client_id`, signing fingerprints, `agconnect-services.json`.
- **Scope discipline:** touch only what the task needs; don't carry one app's conventions into another.

## Verify before "done"

There's no committed CLI build/CI yet, so verification is per-app and mostly manual:
- watch-arkts: emulator runs in Mock mode, Code Linter clean (see its AGENTS.md).
- watch-lite: builds, installs via DevEco Assistant, runs on a real GT (no previewer for P2P).
- companion: `./gradlew test` green; Connect & test lists real HA entities.

State the evidence; if you couldn't run a check, say so — don't assert it passed.
