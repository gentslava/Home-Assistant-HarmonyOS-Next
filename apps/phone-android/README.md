# Phone companion (Android)

The bridge between the watch apps and Home Assistant. The watch has no direct network path to HA:

```
Watch (ArkTS or lite-JS) ──Wear Engine P2P──▶ Phone companion (this) ──HA REST──▶ Home Assistant
```

It holds the HA connection (base URL + long-lived token), translates P2P messages from the watch
into HA REST calls, and replies. Built against the shared contract
[../../docs/p2p-protocol.md](../../docs/p2p-protocol.md) (`v:1`); HA mapping per
[../../docs/ha-integration-notes.md](../../docs/ha-integration-notes.md). MVP spec:
[../../docs/specs/01-real-ha-integration.md](../../docs/specs/01-real-ha-integration.md).

## Status

| Layer | State |
|-------|-------|
| HA REST client (`ha/`) — checkApi, getStates, getState, callService | ✅ done |
| HA → EntityCard mapping + P2P message DTOs (`bridge/`, `p2p/`) | ✅ done |
| P2P↔HA bridge (`bridge/HaBridge.kt`) — transport-agnostic, unit-tested | ✅ done |
| Config UI — HA URL + token, connect & list entities (`MainActivity.kt`) | ✅ done |
| Unit tests (mapper, P2P parse) | ✅ done |
| **Wear Engine P2P transport** (talk to the watch) | ⏳ next — needs AGC credentials |

Right now the app builds, connects to HA, and lists real light/switch/lock entities on the phone.
What's missing for end-to-end is the **Wear Engine transport** that carries `HaBridge` messages to
and from the watch.

## Stack

Kotlin · Jetpack Compose (Material3) · OkHttp + kotlinx-serialization (HA REST) ·
EncryptedSharedPreferences (token at rest) · HMS Wear Engine (Phase 1e).

## Structure

```
app/src/main/java/ru/gentslava/homeassistant/companion/
  MainActivity.kt        Compose config/status screen
  ha/                    HaConfig (secure URL+token), HaClient (REST), HaModels
  bridge/                EntityMapper (HA→EntityCard), HaBridge (P2P msg → HA → reply JSON)
  p2p/                   Messages.kt — protocol DTOs + parseIncoming()
app/src/test/...         EntityMapperTest, MessagesTest
```

## Build & run

Open `apps/phone-android/` in Android Studio (it syncs Gradle). Or:
```bash
./gradlew assembleDebug      # build
./gradlew test               # unit tests (mapper + protocol)
```
Run on the phone, enter HA URL (`http://homeassistant.local:8123`) + a long-lived token
(HA → Profile → Long-Lived Access Tokens), tap **Connect & test** — it should list your entities.

## Enabling Wear Engine P2P (Phase 1e)

Needs a Huawei developer setup (the same kind the watch apps need):
1. Register the app in **AppGallery Connect**, get a `client_id`, download `agconnect-services.json`
   into `app/`.
2. Uncomment in `app/build.gradle.kts`: the `com.huawei.agconnect` plugin and the
   `libs.huawei.wearengine` dependency; uncomment the Wear Engine permissions in `AndroidManifest.xml`.
3. Allow-list the watch apps' bundleName + signing fingerprint, and set the same on the watch side
   (`setRemoteApp(...)` in watch-arkts, `p2pClient.js` in watch-lite).
4. Add a `P2pService` that receives watch messages, calls `HaBridge.handle(...)`, and sends the
   reply back over the Wear Engine `P2pClient`.

> The bridge is intentionally transport-agnostic, so Phase 1e only wires Wear Engine to
> `HaBridge.handle(json) -> json` — no HA logic changes.
