# AGENTS.md — phone-android

Rules for **this app**: the Android **companion** — the bridge between the watch apps and Home
Assistant. Inherits monorepo context in [../../AGENTS.md](../../AGENTS.md) (P2P contract, workflow).
Different stack from the watches (Kotlin, not ArkTS/JS). Open `apps/phone-android/` in Android Studio.

## Quick facts

| | |
|---|---|
| Language / UI | Kotlin, Jetpack Compose (Material3) |
| Build | Gradle (version catalog `gradle/libs.versions.toml`), AGP 8.7, JDK 17 |
| SDK | compileSdk 35, minSdk 26, targetSdk 35 |
| App id | `ru.gentslava.homeassistant.companion` |
| HA | OkHttp + kotlinx-serialization (`Bearer` token) |
| Storage | EncryptedSharedPreferences (HA URL + token) |
| Watch transport | HMS **Wear Engine** (`com.huawei.hms:wearengine`) |
| Tests | JUnit |

## Build / test

```bash
./gradlew assembleDebug      # build (Android Studio also syncs/builds)
./gradlew test               # unit tests
```
First open in Android Studio generates the Gradle wrapper if missing. Run the app, enter HA URL +
long-lived token, **Connect & test** lists real entities.

## Layout & architecture

```
app/src/main/java/ru/gentslava/homeassistant/companion/
  MainActivity.kt        Compose config/status screen; starts the P2P service
  ha/                    HaConfig (secure URL+token) · HaClient (REST) · HaModels
  bridge/                EntityMapper (HA→EntityCard) · HaBridge (P2P msg → HA → reply JSON)
  p2p/                   Messages.kt (protocol DTOs + parseIncoming) · WearEngineP2pService (HMS)
app/src/test/...         EntityMapperTest, MessagesTest
```

**`HaBridge` is transport-agnostic** (`handle(json): json`) — all HA logic lives there, the Wear
Engine service only moves bytes. Keep that seam: new transport ≠ new HA logic. HA mapping reference:
[../../docs/ha-integration-notes.md](../../docs/ha-integration-notes.md).

## Code style (Kotlin)

- Kotlin official style; ktlint defaults. Coroutines for I/O (`withContext(Dispatchers.IO)`), one
  `suspend fun` per HA call, return `Result`.
- DTOs as `data class` with `@Serializable`; field names match the protocol (`@SerialName` for snake_case).
- Compose: stateless composables where practical; state in `remember`/ViewModel.
- **No secrets in code or VCS.** Token entered at runtime → EncryptedSharedPreferences. Wear Engine
  credentials (`PEER_FINGERPRINT`, `agconnect-services.json`) are placeholders / git-ignored.

## Gotchas

- **Wear Engine needs AGC setup** (not just code): register the app in AppGallery Connect, enable
  Wear Engine, add the companion's SHA-256 fingerprint; fill `PEER_FINGERPRINT` in
  `WearEngineP2pService.kt`. Without it P2P won't connect (but the app still builds + talks to HA).
- **`agconnect-services.json` is git-ignored** — a secret; never commit it.
- HA reachability: `GET /api/` → `{"message":"API running."}`; 401 = bad token. The bridge maps HA
  errors to `Ack { ok:false, error }`.
- Protocol change = bump `v` + update [../../docs/p2p-protocol.md](../../docs/p2p-protocol.md) and both watches.
