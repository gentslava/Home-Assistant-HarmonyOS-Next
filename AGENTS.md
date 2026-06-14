# AGENTS.md

Single source of truth for AI coding agents (Claude Code, Cursor, Copilot, Codex, Gemini, …)
working in this repository. Plain Markdown per the [agents.md](https://agents.md/) standard.
`CLAUDE.md` imports this file; do not duplicate content there.

Keep this file lean: include only what an agent can't infer from the code, what it would get
wrong by default, and how this repo expects work to happen. Prune anything that stops being true.

## What this is

A wearable **Home Assistant** client for Huawei watches (Watch GT 4/5/6) on **HarmonyOS Next**,
written in **ArkTS + ArkUI**. The watch has no direct network path to Home Assistant: it talks to
an Android **companion app** over **Wear Engine P2P**, and the companion holds the HA REST
connection. The companion lives in a **separate repository and is not written yet** — the wire
contract in [docs/p2p-protocol.md](docs/p2p-protocol.md) is what both sides build against.

Maturity: clean MVP. Three entity domains (`light`, `switch`, `lock`), EN + RU localization.

## Quick facts

| | |
|---|---|
| Platform | HarmonyOS Next, `wearable` device type, round screen |
| Language / UI | ArkTS, ArkUI (`@kit.ArkUI`), `ArcList`/`ArcScrollBar` |
| SDK | target `6.0.1(21)`, compatible `6.0.0(20)`; minAPI 20 / targetAPI 21 |
| Bundle | `ru.gentslava.homeassistant`, version `1.0.0` |
| State mgmt | ArkUI **V2 only** (`@ComponentV2`/`@ObservedV2`/`@Trace`/`AppStorageV2`) |
| Transport | Wear Engine P2P (`@kit.WearEngine`), UTF-8 JSON, `v:1` protocol |
| Production deps | **none** (pure ArkTS) — keep it that way unless discussed |
| Test/mock libs | `@ohos/hypium`, `@ohos/hamock` (devDependencies) |
| Module | single `entry` module, source under `entry/src/main/ets/` |

## Build / run / test / lint

This project is driven from **DevEco Studio** — there is **no committed `hvigorw` wrapper**, so
prefer the IDE actions below. Do not invent CLI build commands; if a headless `hvigorw`/`ohpm`
CLI is available in the environment, the equivalents are listed but are not the assumed path.

| Task | DevEco Studio | CLI equivalent (only if tooling present) |
|------|---------------|------------------------------------------|
| Install deps | auto on sync | `ohpm install` |
| Build HAP | Build ▸ Build Hap(s)/APP(s) | `hvigorw assembleHap` |
| Run | Run on a **wearable emulator** (lands on Mock — see gotchas) | — |
| Local unit tests | run `entry/src/test/**` (`LocalUnit.test.ets`, `List.test.ets`) | `hvigorw test` |
| Instrumented tests | run `entry/src/ohosTest/**` on emulator/device | — |
| Lint | Code ▸ Code Linter (uses `code-linter.json5`) | `code_linter` |

Test framework is **hypium** (`describe`/`it`/`expect`); `hamock` for mocks. Workflow notes in
[DEVELOPMENT.md](DEVELOPMENT.md).

## Where things live

```
entry/src/main/ets/
  pages/          Index, EntityDetails, Settings, About   (route targets)
  presentation/   store/ (HomeAssistantStore) + ui/components, WatchInsets
  domain/         model/ (EntityCard, EntityAction, P2pMessages) + repository/ (interface)
  data/           repository/ (P2p + Mock) + p2p/ (WearEngineP2pClient, PeerDeviceResolver)
  app/            Services (DI + fallback), AppKeys, AppInfo
  core/           json, log, utils (uid)
entry/src/main/resources/   base/ (EN) + ru_RU/ strings, colors, media; profile/main_pages.json
entry/src/ohosTest/         instrumented tests        entry/src/test/   local unit tests
docs/                       architecture.md, p2p-protocol.md, adr/
```

Full picture: [docs/architecture.md](docs/architecture.md). Decisions: [docs/adr/](docs/adr/).
Contribution process & "add a new HA domain" walkthrough: [CONTRIBUTING.md](CONTRIBUTING.md).

## Architecture in one paragraph

Clean Architecture, dependencies inward: `presentation → domain ← data`. The seam is the
`HomeAssistantRepository` interface, implemented by `P2pHomeAssistantRepository` (real) and
`MockHomeAssistantRepository` (emulator). `Services` (composition root) picks one at startup with a
non-blocking fallback. UI binds to the `@Trace` fields of the single `HomeAssistantStore`. Why it's
shaped this way: [ADR-0001](docs/adr/0001-clean-architecture-layering.md)..[0004](docs/adr/0004-componentv2-appstoragev2-state.md).

## Code style (strict ArkTS — these differ from plain TypeScript)

- **No `any` / `unknown`.** Type everything; the linter is strict.
- **State: V2 decorators only.** Never mix V1 (`@State`/`@Observed`/`@Prop`/`AppStorage`) with V2.
- **Don't pass functions via `@Param`/`@Prop`.** Use other patterns for callbacks.
- **Keep `build()` UI-only.** Avoid `const`/`let` inside `build()` if the linter flags it.
- **Navigation:** prefer the UIContext router (`getUIContext().getRouter()`), not deprecated router APIs.
- **Round screen:** use `ArcList`/`ArcScrollBar` for scrollable content; verify no clipping on a round emulator.
- **No production dependencies** without discussion — the app is intentionally pure ArkTS.
- **i18n:** every user-facing string goes through `$r('app.string.…')` with both `base` (EN) and
  `ru_RU` entries. Never hardcode display text.
- Respect the layer rules: `domain/` imports no framework/`data`/`presentation`; `presentation/`
  never imports `data/` directly.

## Gotchas (non-obvious, will bite you)

- **Emulator always uses Mock.** No paired phone ⇒ `hasConnectedPeer()` is false ⇒ fallback to
  `MockHomeAssistantRepository`. This is by design, not a bug. Any change must keep Mock working.
- **`setRemoteApp(bundleName, fingerprint)` is commented out** in
  [`Services.ets`](entry/src/main/ets/app/Services.ets) — real-device P2P won't connect until the
  companion's bundleName + signing fingerprint are filled in.
- **`module.json5` metadata are placeholders:** `client_id = PUT_YOUR_CLIENT_ID_HERE`,
  `wearEngineRemoteAppNameList = com.yourcompany.ha.bridge`. Don't treat them as real values.
- **`Services.initWithFallback` is fire-and-forget** (not awaited in `EntryAbility`) — intentional,
  so UI never blocks on P2P/network. Don't "fix" it by awaiting.
- **P2P requests correlate by `id` with an 8s timeout**; replies may arrive out of order. Changing
  message shapes is a breaking change — bump `v` and update [docs/p2p-protocol.md](docs/p2p-protocol.md).
- **The companion repo doesn't exist yet.** When work implies phone-side behavior, update the
  protocol contract rather than assuming an implementation.

## Working in this repo

- **Verify before claiming done:** the emulator (Mock mode) must still run, and the linter must
  report no new ArkTS errors/warnings. See the PR checklist in [CONTRIBUTING.md](CONTRIBUTING.md).
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`), one focused change per PR.
- **Process & agent roles** (Explore → Plan → Code → Commit, review/test agents): [DEVELOPMENT.md](DEVELOPMENT.md).
- **Scope discipline:** touch only what the task needs; don't refactor adjacent code or remove
  comments/placeholders (e.g. the commented `setRemoteApp`) you don't fully understand.
