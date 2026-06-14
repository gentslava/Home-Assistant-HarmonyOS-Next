# AGENTS.md — watch-lite

Rules for **this app**: the lite-wearable JS Home Assistant client for **Huawei Watch GT 4/5/6**.
Inherits the monorepo-wide context in [../../AGENTS.md](../../AGENTS.md) (P2P contract, workflow) —
this file adds the lite-wearable specifics. **This is a different runtime from `watch-arkts`**, not
ArkTS — do not apply ArkTS/ArkUI rules here. Why GT needs this:
[../../docs/platform-constraints.md](../../docs/platform-constraints.md).

Open `apps/watch-lite/` as the project root in DevEco Studio.

## Quick facts

| | |
|---|---|
| Model | FA (`config.json`, `deviceType: liteWearable`), `MainAbility` |
| Language | **JavaScript limited to ES5.1** (JerryScript runtime) |
| UI | HML + CSS + JS (Vue-like); **no ArkUI, no `ArcList`** |
| Bundle | `ru.gentslava.homeassistant` |
| Transport | Wear Engine P2P via bundled `wearengine.js` SDK (callbacks) |

## 🔴 Limited JS — JerryScript supports a fixed ES6 subset, not modern JS

**Not supported — these throw or break install, never use:**

- ❌ `globalThis` (use `getApp().data` / the `store.js` singleton)
- ❌ `Promise`, `async`/`await` — use callbacks `{ onSuccess, onFailure }`
- ❌ spread `...arr`, optional chaining `?.`, nullish `??`

**Supported ES6 subset** (verified on device): `let`/`const`, arrow functions, `class`,
destructuring, template strings, `for-of`, rest params, enhanced object literals. Existing files mix
`var`/`function` and `const`/arrow — both are fine; match the file you're editing.

Cross-page state goes through `common/store.js` (`getStore()`), never `globalThis`.

## Build / install (no direct hdc on GT)

GT has no "HDC debugging"/"Debug via Wi-Fi" — install goes **through the phone**:

1. Build a **signed** HAP in DevEco (debug cert + the watch UDID in the `.p7b` profile via AppGallery Connect).
2. Push to the phone, install via **HUAWEI DevEco Assistant** (watch paired, Huawei Health running).
3. Logs: **查看日志 / View Logs** in DevEco Assistant. No system `hdc`/`hilog` on GT.

`@system.wearengine` exists only on a **real watch**, not the DevEco previewer — P2P can't be tested in preview.

## Layout

```
entry/src/main/
  config.json                 FA manifest (liteWearable) + metaData.customizeData "supportLists"
  js/MainAbility/
    app.js                    app entry (onCreate/onShow)
    pages/{index,entity,settings,about}/  index.hml + .css + .js
    common/
      store.js                getStore() singleton (getApp().data anchor — NOT globalThis)
      services/               p2pClient.js (Wear Engine), haRepository.js (repo over P2P)
      wearenginesdk/          wearengine.js — copied SDK (not a dependency)
      constants/, utils/, icons/
    i18n/                     en-US.json, ru-RU.json
```

## Gotchas (lite wearable)

- **HAP ≤ 10 MB · page ≤ 48 KB · ~1 MB RAM · no background · router has no back-stack** (use
  `router.replace`, not push) · **state not restored** after restart.
- **Icons bloat:** the lite packer decodes PNG to **raw RGBA** in the `.bin` — a large PNG can blow
  the 10 MB limit. Keep icons tiny (originals in `_icon_backup/`).
- **No internet on the watch** — all network goes through the companion over Wear Engine P2P
  (`services/p2pClient.js`); `haRepository.js` is the repo on top.
- **Wear Engine SDK is copied in**, not a dependency: `common/wearenginesdk/wearengine.js`. Peer
  config: `setPeerPkgName`/`setPeerFingerPrint` in `p2pClient.js` **and** `supportLists`
  (`"<androidPkg>:<fingerprint>"`) in `config.json` `metaData.customizeData`.
- **P2P is callbacks-only** with manual id-correlation (see `p2pClient.js`); 8s timeout; keep
  messages small (a few KB). Protocol = [../../docs/p2p-protocol.md](../../docs/p2p-protocol.md).
- **Lite list won't render after array mutation** — reassign the array reference (`this.items = this.items.slice(0)`) to force a re-render.
