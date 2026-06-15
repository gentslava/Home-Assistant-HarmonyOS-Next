# AGENTS.md — watch-arkts

Rules for **this app**: the ArkTS Home Assistant client for the full-wearable class
(Huawei Watch 4/5/Ultimate). Inherits the monorepo-wide context in [../../AGENTS.md](../../AGENTS.md)
(P2P contract, workflow, scope discipline) — this file adds only what's ArkTS-specific.

Open `apps/watch-arkts/` as the project root in **DevEco Studio** (git root is the monorepo root).

## Quick facts

| | |
|---|---|
| Language / UI | ArkTS, ArkUI (`@kit.ArkUI`), `ArcList`/`ArcScrollBar` |
| Model | Stage (`UIAbility`, `module.json5`), single `entry` module |
| SDK | target `6.0.1(21)`, compatible `6.0.0(20)`; minAPI 20 / targetAPI 21 |
| Bundle | `ru.gentslava.homeassistant`, version `1.0.0` |
| State mgmt | ArkUI **V2 only** (`@ComponentV2`/`@ObservedV2`/`@Trace`/`AppStorageV2`) |
| Transport | Wear Engine P2P (`@kit.WearEngine`), UTF-8 JSON, `v:1` protocol |
| Production deps | **none** (pure ArkTS) |
| Test/mock libs | `@ohos/hypium`, `@ohos/hamock` |

## Build / run / test / lint (DevEco Studio)

No committed `hvigorw` — drive from the IDE. Paths below are app-relative (project root = this app).

| Task | DevEco Studio | CLI (only if tooling present) |
|------|---------------|-------------------------------|
| Build HAP | Build ▸ Build Hap(s)/APP(s) | `hvigorw assembleHap` |
| Run | Run on a **wearable emulator** (lands on Mock — see gotchas) | — |
| Local unit tests | run `entry/src/test/**` (`LocalUnit.test.ets`, `List.test.ets`) | `hvigorw test` |
| Instrumented tests | run `entry/src/ohosTest/**` on emulator/device | — |
| Lint | Code ▸ Code Linter (`code-linter.json5`) | `code_linter` |

Tests use **hypium** (`describe`/`it`/`expect`), `hamock` for mocks.

## Layout

```
entry/src/main/ets/
  pages/          Index, EntityDetails, Settings, About
  presentation/   store/ (HomeAssistantStore) + ui/components, WatchInsets
  domain/         model/ (EntityCard, EntityAction, P2pMessages) + repository/ (interface)
  data/           repository/ (P2p + Mock) + p2p/ (WearEngineP2pClient, PeerDeviceResolver)
  app/            Services (DI + fallback), AppKeys, AppInfo
  core/           json, log, utils (uid)
```

Architecture map: [../../docs/architecture.md](../../docs/architecture.md). Decisions:
[../../docs/adr/](../../docs/adr/).

## Code style (strict ArkTS — differs from plain TypeScript)

- **No `any` / `unknown`.** Type everything; the linter is strict.
- **State: V2 decorators only.** Never mix V1 (`@State`/`@Observed`/`@Prop`/`AppStorage`) with V2.
- **Don't pass functions via `@Param`/`@Prop`.**
- **Keep `build()` UI-only**; avoid `const`/`let` in `build()` if the linter flags it.
- **Navigation:** UIContext router (`getUIContext().getRouter()`), not deprecated router APIs.
- **Round screen:** `ArcList`/`ArcScrollBar` for scroll; verify no clipping on a round emulator.
- **No production dependencies** without discussion.
- **i18n:** every user-facing string via `$r('app.string.…')` with `base` (EN) + `ru_RU` entries.
- Layer rules: `domain/` imports no framework/`data`/`presentation`; `presentation/` never imports `data/`.

## Gotchas

- **Emulator always uses Mock.** No paired phone ⇒ `hasConnectedPeer()` false ⇒ `MockHomeAssistantRepository`. By design; keep Mock working.
- **`setRemoteApp(bundleName, fingerprint)` is commented out** in `entry/src/main/ets/app/Services.ets` — real P2P won't connect until the companion's bundleName + fingerprint are filled in.
- **`module.json5` metadata are placeholders:** `client_id`, `wearEngineRemoteAppNameList`.
- **`Services.initWithFallback` is fire-and-forget** (not awaited) — UI never blocks on P2P.
- **P2P correlates by `id`, 8s timeout**; replies may arrive out of order. Protocol change = bump `v` + update [../../docs/p2p-protocol.md](../../docs/p2p-protocol.md).
