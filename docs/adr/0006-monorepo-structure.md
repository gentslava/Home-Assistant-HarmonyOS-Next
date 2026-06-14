# 0006. Monorepo for the watch apps and companion

- **Status:** Accepted
- **Date:** 2026-06-14

## Context

[ADR-0005](0005-target-full-wearable-not-lite.md) split the product into separate runtimes: an ArkTS
app for full wearables (Watch 4/5/Ultimate) and a lite-JS app for Watch GT, plus a future Android
companion. These three need to evolve against **one shared P2P contract** and one domain model. The
question was whether to keep them in one repo or three.

Key facts that drove the decision:
- The two watch apps share **no code** (ArkTS→PANDA bytecode vs lite-JS→haptobin; different
  compilers/engines). They share only the **contract, domain model, and architecture** — documents,
  not binaries.
- The companion is a different stack (Kotlin) and crosses a repo boundary regardless.
- A HarmonyOS DevEco project expects its config at the project root — but DevEco (like Android
  Studio) **opens a subfolder as the project root**, with git one level up. So a monorepo with
  `apps/<app>/` is fully workable; the "one project per root" concern does not apply.

## Decision

We use a **single monorepo** with one app per subfolder and a shared root layer:

```
apps/
  watch-arkts/    ArkTS app (full wearable)      — open as DevEco project root
  watch-lite/     lite-JS app (Watch GT)         — open as DevEco project root
  phone-android/  Android companion (Kotlin)     — open in Android Studio (placeholder)
docs/             architecture, p2p-protocol, platform-constraints, adr/   (shared)
AGENTS.md  CLAUDE.md  DEVELOPMENT.md  .claude/                              (shared AI layer)
```

The shared **P2P contract lives once** at `docs/p2p-protocol.md`; all apps build against it.

## Consequences

- The contract can't drift between apps — it's a single file, changed atomically with both sides.
- One clone, one history, one AIDD layer, one issue tracker.
- Each IDE opens its app subfolder; the git root (monorepo root) is one level up.
- `.gitignore` is recursive (`**/build/`, `**/.hvigor/`, …) to cover every `apps/*`.
- Paths in docs are **repo-root-relative for clickable links** (`apps/watch-arkts/entry/…`) and
  **app-relative for in-IDE actions** (`entry/src/test`, since the DevEco project root is the app).
- If CI is added, it must be path-filtered per app (different toolchains: hvigor vs lite vs Gradle).
- Cost paid once: the original ArkTS project was moved from the repo root into `apps/watch-arkts/`
  (via `git mv`, history preserved) and doc paths were updated.

## Alternatives considered

- **Three separate repos** — rejected: the only real shared thing (the contract) would have to be
  synced by hand across repos, the main pain point for a solo maintainer; the monorepo's main
  downside (DevEco "one project per root") turned out not to exist (subfolder-as-root works).
- **Hybrid (monorepo for the two watch apps, companion separate)** — rejected: still leaves the
  contract crossing a repo boundary to the companion; full monorepo keeps all three in sync.
