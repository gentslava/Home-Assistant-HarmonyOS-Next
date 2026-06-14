# Home Assistant for Huawei Wearables — monorepo

A [Home Assistant](https://www.home-assistant.io/) client for Huawei smartwatches, plus its phone
companion. The watch has no direct network path to Home Assistant — it talks to an Android companion
over **Wear Engine P2P**, and the companion holds the HA REST connection.

Huawei splits watches into two incompatible developer platforms, so there are **two watch apps**
sharing one design and one wire contract — see [docs/platform-constraints.md](docs/platform-constraints.md).

## Apps

| Path | What | Target | Stack |
|------|------|--------|-------|
| [`apps/watch-arkts/`](apps/watch-arkts/) | Full-wearable watch app | **Watch 4/5/Ultimate** | ArkTS + ArkUI (Stage), `ArcList` |
| [`apps/watch-lite/`](apps/watch-lite/) | Lite-wearable watch app | **Watch GT 4/5/6** | JS (FA), HML/CSS, JerryScript (ES5.1) |
| [`apps/phone-android/`](apps/phone-android/) | Phone companion (P2P ↔ HA REST) | Android | Kotlin + Compose — *not written yet* |

The two watch apps are **mirror architectures in different runtimes** — they share the
[P2P protocol](docs/p2p-protocol.md), the domain model, and the layering, but **no code**
(ArkTS compiles to `PANDA` bytecode, lite-JS to a haptobin `.bin` — different engines).

## Why two apps

Watch GT (4/5/6) is a **lite wearable**: it physically rejects third-party ArkTS Stage HAPs
(`Failed to decompress`, verified on a GT 6) and only runs lite-JS. Full ArkTS/ArkUI — and a native
`ArcList` round-screen UI — exists only on the **Watch 4/5/Ultimate** class. The full story, with
on-device and byte-level evidence, is in [docs/platform-constraints.md](docs/platform-constraints.md)
and [ADR-0005](docs/adr/0005-target-full-wearable-not-lite.md).

## Repository layout

```
apps/                this monorepo's three apps (open each as its own project root in its IDE)
docs/
  architecture.md      system map (watch-arkts)
  p2p-protocol.md      watch ↔ phone wire contract (shared, versioned)
  platform-constraints.md   full wearable vs lite — the device-platform truth
  adr/                 architecture decision records
AGENTS.md  CLAUDE.md  DEVELOPMENT.md   AI + workflow layer (see below)
CONTRIBUTING.md
```

## Working in this repo (AI-managed)

This repository is set up for AI-first development. Start here:

- [AGENTS.md](AGENTS.md) — single source of truth for AI agents (commands, layout, rules, gotchas).
- [CLAUDE.md](CLAUDE.md) — Claude Code entry point (imports AGENTS.md).
- [DEVELOPMENT.md](DEVELOPMENT.md) — the Explore → Plan → Code → Commit workflow and role subagents.
- [docs/adr/](docs/adr/) — why key decisions were made (don't re-litigate; supersede).

## License

MIT.
