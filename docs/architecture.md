# Architecture

High-level map of the watch app. Read this before touching code that crosses layers
(UI ↔ store ↔ repository ↔ transport). For the wire format between watch and phone,
see [p2p-protocol.md](p2p-protocol.md). For *why* things are the way they are, see [adr/](adr/).

> **Scope.** This document describes **`apps/watch-arkts/`** — the ArkTS app for the full wearable
> class (Watch 4/5/Ultimate). The repo is a [monorepo](adr/0006-monorepo-structure.md); the lite-JS
> app for Watch GT (`apps/watch-lite/`) mirrors this architecture in a different runtime
> ([platform-constraints.md](platform-constraints.md)).

## What this app is

A wearable Home Assistant client for Huawei **Watch 4/5/Ultimate** (HarmonyOS Next, ArkTS + ArkUI).
The Watch **GT** series is not a target — it runs as lite wearable; see
[platform-constraints.md](platform-constraints.md).
The watch has **no direct network access to Home Assistant**. It talks to an Android
**companion app** over Wear Engine P2P; the companion holds the HA REST connection.

```
┌──────────────────────────┐   Wear Engine P2P    ┌────────────────────┐   HA REST    ┌────────────────┐
│  Watch app (this repo)   │  (UTF-8 JSON msgs)   │  Android companion │  (HTTP/JSON) │ Home Assistant │
│  HarmonyOS / ArkTS       │ <══════════════════> │  (separate repo)   │ <══════════> │   instance     │
└──────────────────────────┘                      └────────────────────┘              └────────────────┘
```

The companion app lives in a **separate repository** and is not written yet. The P2P
message protocol is therefore a forward-looking **contract** that both sides must honor —
treat [p2p-protocol.md](p2p-protocol.md) as the source of truth when building either side.

## Layers (Clean Architecture)

Source root: `apps/watch-arkts/entry/src/main/ets/`. Dependencies point inward: `presentation → domain ← data`.
`domain` knows nothing about ArkUI, Wear Engine, or JSON.

```
presentation/   UI + global state           depends on domain (models, actions)
  pages/          Index, EntityDetails, Settings, About (route targets)
  store/          HomeAssistantStore  (@ObservedV2/@Trace global state)
  ui/             components (EntityRow, SystemHeader, SystemTile, IconBadge), WatchInsets
        │
        ▼
domain/         business core (pure ArkTS, no framework imports)
  model/          EntityCard, EntityAction, P2pMessages (DTOs)
  repository/     HomeAssistantRepository  (interface — the seam)
        ▲
        │ implemented by
data/           outward adapters
  repository/     P2pHomeAssistantRepository (real), MockHomeAssistantRepository (emulator)
  p2p/            WearEngineP2pClient (transport), PeerDeviceResolver

app/            composition root: Services (DI + fallback), AppKeys, AppInfo
core/           framework-agnostic helpers: json (toJson/fromJson), log (Logger), utils (uid)
```

| Layer | Rule |
|-------|------|
| `domain/` | No imports from `data/`, `presentation/`, or HarmonyOS kits. Plain types only. |
| `data/` | Implements `domain` interfaces. Owns all Wear Engine / encoding details. |
| `presentation/` | Talks to `domain` types and the store. Never imports `data/` directly. |
| `app/` | The only place that wires a concrete repository to the interface (`Services`). |

The seam is `HomeAssistantRepository` ([interface](../apps/watch-arkts/entry/src/main/ets/domain/repository/HomeAssistantRepository.ets)).
Swapping P2P for Mock — or, later, a direct-REST or cloud transport — touches only `data/` and `Services`.

## Startup flow

`EntryAbility.onWindowStageCreate` ([source](../apps/watch-arkts/entry/src/main/ets/entryability/EntryAbility.ets)):

1. `AppStorageV2.connect(HomeAssistantStore, …)` — register the global store.
2. `windowStage.loadContent('pages/Index')` — render UI immediately.
3. `Services.initWithFallback(this.context)` — **fire-and-forget, not awaited**.

Step 3 is intentionally not awaited: **UI must never block on P2P init or the network**
(see [ADR-0002](adr/0002-repository-pattern-mock-fallback.md) and CONTRIBUTING rule #2).

`Services.initWithFallback` ([source](../apps/watch-arkts/entry/src/main/ets/app/Services.ets)):

```
tryInitRealRepo(ctx, 1200ms)            ── on any failure / timeout ──▶  new MockHomeAssistantRepository()
  ├─ new WearEngineP2pClient(ctx)
  ├─ p2p.init()
  ├─ hasConnectedPeer()  (timeout 1200ms) ── no peer ──▶  throw ──▶ fallback to Mock
  └─ P2pHomeAssistantRepository.init()    (timeout 1200ms)
                          │
                          ▼
              Services.repo = <chosen repo>
              store.sync()   ── populate UI
```

On the watch **emulator** there is no paired phone, so `hasConnectedPeer()` is false and the
app always lands on `MockHomeAssistantRepository`. This is the supported dev path.

## Runtime data flow

The store is the single entry point for the UI. UI components call store methods and read
`@Trace` fields; they never touch a repository directly.

| User action | Store method | Repository call | P2P round-trip |
|-------------|--------------|-----------------|----------------|
| App opens / list refresh | `sync()` | `repo.sync()` | `SYNC_REQUEST` → `SYNC_RESPONSE` |
| Refresh one entity | `refreshEntity(id)` | `repo.syncEntityCard(id)` | `SYNC_ENTITY_REQUEST` → `SYNC_ENTITY_RESPONSE` |
| Tap an action | `runAction(action)` | `repo.callAction(action)` | `CALL_SERVICE` → `ACK` |

`HomeAssistantStore` ([source](../apps/watch-arkts/entry/src/main/ets/presentation/store/HomeAssistantStore.ets)) exposes three
`@Trace` fields the UI binds to: `cards`, `isSyncing`, `lastError`. State management uses the
**V2 family only** (`@ObservedV2`/`@Trace`/`@ComponentV2`/`AppStorageV2`) — see
[ADR-0004](adr/0004-componentv2-appstoragev2-state.md). Do not mix V1 (`@State`/`@Observed`) decorators.

In `P2pHomeAssistantRepository` ([source](../apps/watch-arkts/entry/src/main/ets/data/repository/P2pHomeAssistantRepository.ets)),
each request gets a unique `id` and is parked in a `pending` map until the matching reply arrives,
with an **8s timeout** per request. Correlation is by `id`, so replies may arrive out of order.

## Domain model

`EntityCard` ([source](../apps/watch-arkts/entry/src/main/ets/domain/model/EntityCard.ets)) is what the UI renders;
`EntityAction` is a self-describing HA service call (`domain` + `service` + `data`). The watch is
**presentation-only**: the companion decides which actions a card exposes, the watch just renders
them and echoes the action back on tap. Adding a new HA domain is therefore mostly a
companion-side + UI-mapping change, not a new code path here. See "Adding a new entity domain"
in [CONTRIBUTING.md](../CONTRIBUTING.md).

## Round-screen UI

Scrollable content uses `ArcList`/`ArcScrollBar` (not `List`) so it follows the circular bezel
like system apps. `WatchInsets` provides safe insets. Verify every UI change on a round emulator —
no clipped text or buttons at the top/bottom arcs.

## Extension points (where future work plugs in)

- **New HA domain** (`light`/`switch`/`lock` today): UI mapping (icon/color/labels) + DTO/actions
  + companion REST + mock entities. Walkthrough in [CONTRIBUTING.md](../CONTRIBUTING.md).
- **Remote access** (cloud / external URL + TLS): a new `HomeAssistantRepository` implementation
  in `data/`, selected in `Services`. The UI and domain stay untouched.
- **On-watch connection config**: currently the companion owns base URL + token; a settings UI
  on the watch would feed configuration over P2P.
```
