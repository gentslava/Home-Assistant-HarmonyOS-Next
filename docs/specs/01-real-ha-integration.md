# Spec 01 — Real Home Assistant integration (MVP)

Status: **Draft** · Owner: gentslava · Created: 2026-06-14

Turns the two watch UIs (currently mock/hardcoded) into a real Home Assistant assistant by adding
the Android **companion** and wiring both watches to it over Wear Engine P2P.

## Objective

A user with a Huawei watch (GT 6 → lite, or Watch 4/5 → ArkTS) and an Android phone can:
1. Configure their Home Assistant once on the phone (base URL + long-lived token).
2. See their **real** HA entities on the watch.
3. Toggle them from the watch and have it **actually** change state in HA.

Success = no mock data in the normal path; a tap on the watch flips a real light/switch/lock in HA.

## Tech Stack

| App | Stack | Target |
|-----|-------|--------|
| `apps/watch-arkts` | ArkTS + ArkUI (Stage), Wear Engine P2P | Watch 4/5/Ultimate |
| `apps/watch-lite` | JS (ES5.1, FA), HML/CSS, Wear Engine P2P | Watch GT 4/5/6 |
| `apps/phone-android` | Kotlin + Jetpack Compose, HMS **Wear Engine** SDK, OkHttp | Android phone |

HA side: [REST API](https://developers.home-assistant.io/docs/api/rest/) — `GET /api/states`,
`POST /api/services/<domain>/<service>`, auth via `Authorization: Bearer <token>`. Verified API
facts and the HA→P2P mapping live in [ha-integration-notes.md](../ha-integration-notes.md).

## Commands

- **Watches:** DevEco Studio — open `apps/watch-arkts/` or `apps/watch-lite/` as project root;
  Build → Build Hap(s); install (lite → DevEco Assistant, arkts → run/hdc). No committed `hvigorw`.
- **Companion:** Android Studio — open `apps/phone-android/`; `./gradlew assembleDebug`;
  install on the phone (must have HMS Core + paired watch).

## Project Structure (additions)

```
apps/phone-android/                 NEW — Kotlin companion
  app/src/main/java/.../
    p2p/        Wear Engine receiver + sender (HiWear/Wear Engine SDK)
    ha/         HA REST client (OkHttp) + models
    bridge/     P2P message ↔ HA REST translation (the core)
    ui/         Compose: HA config screen (URL + token), status
  app/src/main/AndroidManifest.xml
  build.gradle.kts, settings.gradle.kts

apps/watch-lite/.../common/services/
  p2pClient.js      FILL IN — Wear Engine P2P (send/receive JSON)
  haRepository.js   FILL IN — repository over p2pClient (sync / syncEntity / callAction)

apps/watch-arkts/.../app/Services.ets   enable real P2P (uncomment setRemoteApp, real client_id)
docs/p2p-protocol.md                    extend: handshake/config, connection error codes
```

## Code Style

- **watch-arkts:** strict ArkTS, ArkUI V2 only — see [AGENTS.md](../../AGENTS.md).
- **watch-lite:** ES5.1 only (no `globalThis`/`?.`/`??`/spread/`async`); `function(){}`; state via
  `getApp().data`/singleton. Match existing files.
- **companion (Kotlin):**
  ```kotlin
  // Coroutines for I/O; one suspend fun per HA call; data classes for DTOs matching p2p-protocol.md
  suspend fun callService(domain: String, service: String, data: Map<String, String>): Result<Unit> =
      withContext(Dispatchers.IO) { /* OkHttp POST /api/services/$domain/$service */ }
  ```
  Kotlin official style; ktlint defaults; no secrets in code (token entered at runtime, stored in
  EncryptedSharedPreferences).

## Testing Strategy

- **companion:** JUnit for the bridge (P2P msg → HA call mapping) and HA-client DTO parsing
  (mock HTTP). The translation layer is the highest-value test target.
- **watches:** hypium (arkts) / manual on device (lite). Test the repository's message
  correlation and the domain→action mapping.
- A task is done only with evidence (test output or on-device behavior), per DEVELOPMENT.md.

## Boundaries

- **Always:** keep the mock path working as an offline/dev fallback; bump `v` + update both watch
  apps when changing [p2p-protocol.md](../p2p-protocol.md); keep watch-lite ES5.1-clean.
- **Ask first:** adding domains beyond the current set (light/switch/lock/cover/scene/sensor);
  switching to HA WebSocket; adding a production dependency to a watch app (dependency-free today).
- **Never:** commit HA tokens / `client_id` / signing fingerprints; hardcode a user's HA URL.

## Success Criteria

1. Companion: enter HA URL + token → app lists real entities from `GET /api/states` (filtered to
   light/switch/lock).
2. Companion ↔ watch: a `SYNC_REQUEST` over P2P returns real `EntityCard[]`; watch renders them.
3. Watch → HA: tapping an action sends `CALL_SERVICE` → companion calls HA → real device changes;
   watch reflects new state after `SYNC_ENTITY`.
4. UI states: watch shows loading, empty, and offline/error (not silent mock) when companion is
   unreachable.
5. Both watch apps work against the same companion and the same `v:1` (+ extensions) protocol.

## Resolved (per ha-integration-notes.md)

- Auth: **long-lived token** (`Bearer`), validity check `GET /api/` → `{"message":"API running."}`.
- Freshness: **MVP = REST** (`GET /api/states` on sync, `POST /api/services/...` on action). HA
  WebSocket + cache is a Phase-4 improvement (and live push to the watch needs a protocol `v` bump).
- `CALL_SERVICE` → `POST /api/services/<domain>/<service>` is a **direct pass-through** (body = `data`).
- Entity selection: MVP shows **all** light/switch/lock; favorites later.

## Open Questions

- **Wear Engine on Android = HMS Wear Engine SDK** — requires a Huawei Developer app (`client_id` in
  AGC) + the watch app's bundleName/fingerprint allow-list. Confirm you have AGC access to register
  the companion app and obtain `client_id`. (Blocks real P2P pairing, not the build or HA layer.)
