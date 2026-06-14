# Home Assistant — Watch GT (lite wearable)

Lite-wearable Home Assistant client for **Huawei Watch GT 4/5/6**. This is the **lite track**: a
different runtime from the ArkTS app in [`../watch-arkts/`](../watch-arkts/) — see
[platform-constraints.md](../../docs/platform-constraints.md) for why GT needs this.

## Stack

- **FA model**, `config.json`, `deviceType: liteWearable`.
- **JavaScript limited to ES5.1** (no `globalThis`, `?.`, `??`, spread, `async`/`await`, `Promise`).
- UI: **HML + CSS + JS** (no ArkUI, no `ArcList`). Engine: **JerryScript / ACE Lite**.
- Bundle `ru.gentslava.homeassistant` (shared with the ArkTS app).

## Layout

```
entry/src/main/
  config.json                 FA manifest (liteWearable)
  js/MainAbility/
    app.js                    app entry (onCreate / onShow)
    pages/{index,entity,settings,about}/  index.hml + .css + .js
    common/
      store.js                shared state (via getApp()/singleton module — NOT globalThis)
      services/               haRepository.js, p2pClient.js
      repo/                   MockHomeAssistantRepository.js
      constants/, utils/, icons/
    i18n/                     en-US.json, ru-RU.json
  resources/                  app icon, strings
```

This **mirrors** the ArkTS app's architecture (store / repository / mock / P2P / 4 pages /
EN+RU) — same design, reimplemented in lite-JS. The shared contract is
[../../docs/p2p-protocol.md](../../docs/p2p-protocol.md).

## Hard limits (lite wearable)

HAP ≤ 10 MB · page ≤ 48 KB · ~1 MB RAM · no background · router has no back-stack · state not
restored after restart · images decoded to raw RGBA (keep icons tiny). Full list:
[platform-constraints.md](../../docs/platform-constraints.md).

## Build & install (no direct hdc on GT)

GT has no "HDC debugging" / "Debug via Wi-Fi", so install goes **through the phone**:

1. Open `apps/watch-lite/` as the project root in DevEco Studio; build a **signed** HAP (debug
   cert + the watch UDID in the `.p7b` profile, via AppGallery Connect).
2. Push the HAP to the phone, install with **HUAWEI DevEco Assistant** (watch paired, Huawei Health
   running).
3. Logs: **查看日志 / View Logs** in DevEco Assistant (no system `hdc`/`hilog` on GT).

## Gotchas

- **ES5.1 only.** Share state via `getApp().data` / a singleton module — `globalThis` throws
  `ReferenceError` on JerryScript.
- **Watch has no internet.** All network goes through the phone companion over Wear Engine P2P
  (`common/services/p2pClient.js`); `haRepository.js` is currently a mock.
- **Icons bloat.** A PNG is decoded to raw RGBA in the `.bin` — a large PNG can blow the 10 MB HAP
  limit. Keep icons small (originals kept in `_icon_backup/`).
