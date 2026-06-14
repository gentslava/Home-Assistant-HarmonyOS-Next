# Platform constraints: full wearable vs lite wearable

Everything we established about which Huawei watches can run this app, with on-device and
byte-level evidence. Read this before assuming anything about the target device — it overrides
older statements (including the original README) that called the Watch GT series a target.

## TL;DR

- **Target device class: full wearable** — Huawei **Watch 4 / 5 / Ultimate** (HarmonyOS Next,
  ArkTS Stage Model, ArkUI). This is what the ArkTS code in this repo runs on.
- **Huawei Watch GT 4 / 5 / 6 are `lite wearable`** for third-party developers. They **cannot run
  third-party ArkTS Stage apps** — only lite JS (FA model, HML/CSS/JS, JerryScript). Verified on a
  physical GT 6 (firmware 6.0.0.188), 2026-06-14.
- A lite (JS) Home Assistant client for GT is a **separate track** (different language + runtime),
  not a build variant of this project. What is shared between tracks is the **architecture and the
  [P2P protocol contract](p2p-protocol.md)**, not the binary.

## Two HarmonyOS runtimes for watches

These are two different languages → compilers → bytecode formats → engines. They are not
interchangeable; the source looking "JS-like" in both cases is superficial.

| | **Full wearable** (this repo) | **Lite wearable** (GT series) |
|---|---|---|
| Model | Stage (`UIAbility`, `module.json5`) | FA (`MainAbility`, `config.json`) |
| `deviceType` | `wearable` | `liteWearable` |
| Language | ArkTS (TypeScript+, decorators, static types) | JS limited to **ES5.1** (no `globalThis`, `?.`, `??`, spread, `async`/`await`, `Promise`) |
| UI | ArkUI declarative (`@Component`/`build()`, `ArcList`) | HML + CSS + JS (Vue-like); **no `ArcList`** |
| Compiler | ArkCompiler → `.abc` | haptobin → `.bin` |
| Bytecode magic | `PANDA` (`50 41 4E 44 41`) | haptobin header (`be 00 00 00 …` + bundleName) |
| Engine / VM | ARK JS Runtime (`ark13.0.1.0`), full ACE | JerryScript, ACE Lite |
| OS tier | standard-ish HarmonyOS | LiteOS mini-system |
| Devices | Watch 4 / 5 / Ultimate | Watch GT 3/4/5/6, Band, Fit |

## Evidence

This is not inference — it was confirmed directly.

**Field test (physical GT 6, firmware 6.0.0.188, 2026-06-14):**
- A minimal, correctly signed ArkTS Stage HAP fails to install with:
  `Installation failed: Failed to decompress entry-default-signed.hap`.
  The **signature passed** (no `9568xxx` signature errors) — it died at the *decompress/format*
  stage. The device's lite installer cannot unpack the Stage container. → **format/runtime
  absence**, not a policy gate.
- A lite (FA/JS) HAP installs and runs on the same watch (`Application onCreate` in the logs).
- DevEco *does* offer an ArkTS Stage target for "Wearable" — so the barrier is **in the device,
  not the toolchain**.

**Byte-level comparison of the two builds** (`lite.zip` / `wearable.zip`):
- `wearable.hap` → `ets/modules.abc` starts with `PANDA` (ARK bytecode, VM `ark13.0.1.0`), plus a
  Stage `module.json` (`virtualMachine: ark13.0.1.0`, `compileMode: esmodule`, `deviceType:
  wearable`) and normal `resources/`.
- `lite.hap` → a single `entry-default-signed.bin` (haptobin container; JerryScript code + images
  decoded to **raw RGBA bitmaps**, which is why lite HAPs bloat — a 139 KB PNG became ~17 MB).

The GT 6 installer expects the haptobin `.bin`; the Stage HAP carries `PANDA` `.abc` in a Stage
zip structure → "Failed to decompress". This is the physical root cause.

## Why every bypass fails (do not re-attempt)

All four explored paths hit the same wall: **third-party code on GT 6 gets only the JerryScript /
ACE-Lite sandbox, which is isolated from the system ARK Runtime / full ACE** where ArkTS and
`ArcList` live. The boundary is a separate runtime + privileges + firmware, not a flag.

| Attempt | Why it's blocked |
|---|---|
| Hack-install the Stage HAP | Device can't unpack the format (field test) |
| Sign-as-lite / run-as-ArkTS | "System" needs Huawei's platform key + `hos_system_app` APL; installer verifies it |
| Mimic the ArkTS build as lite | No `.abc`→JerryScript converter; ArkUI ≠ HML; language must drop to ES5.1 = a full rewrite |
| Embed ArkTS components in a lite app | JerryScript has no `.abc` loader; no NAPI on mini-system; `libace` is privileged, no `.so` path |
| Bootloader / custom firmware | Locked since 2018; no GT root/port exists; brick risk |

Exotic paths were also considered and rejected — listed so they are not re-proposed: **root /
firmware patch** (no retail unlock, can't write to system, brick risk); **0-day jailbreak** (a
separate multi-week security project, none exists publicly for GT 6, breaks on every OTA, and still
needs ArkTS integrated afterward); **an `.abc` interpreter written on top of lite JS** (a VM-in-a-VM
in ~1 MB with no access to `libace` — absurd). None yields a practical result.

The system UI *does* render with native ArkUI (proven: `ArcList` edge-scaling is impossible on the
lite framework) — but that runtime is behind the wall, with no door for third-party code.

## Lite wearable limits (reference for the GT/lite track)

If a lite-JS client is built for GT, these constraints apply:

- HAP ≤ 10 MB; page ≤ 48 KB; ~1 MB RAM; no background; router has no back-stack; state not
  restored after restart.
- Images are decoded to raw RGBA → keep icons tiny (a few PNGs, no large graphics).
- ES5.1 only — share state via a `getApp().data` / singleton module, **not** `globalThis`.
- No watch-side internet → all network goes through the phone companion over Wear Engine P2P.
- No direct `hdc` (GT has no "HDC debugging"/"Debug via Wi-Fi"). Install + logs via the phone app
  **HUAWEI DevEco Assistant** (`查看日志` / View Logs).

## Future outlook

No Huawei roadmap opens third-party ArkTS on the GT series. HarmonyOS 6.1.0.117 (GT 6, 2026) is a
user-feature update only. The unification trend favors ArkTS-on-everything, but via the **Watch**
line, not GT — treat GT→ArkTS as unconfirmed speculation. Re-test signal: an ArkTS-Stage wearable
template *for GT specifically* in DevEco, or a watch-side AppGallery for GT.

## What is portable between the two tracks

Not the binary — the design:

- **[P2P protocol](p2p-protocol.md)** — identical contract; Wear Engine P2P works in both FA-lite
  and ArkTS-Stage.
- **Domain model** (`EntityCard`, `EntityAction`, message DTOs) — same data shapes, ported by hand.
- **Architecture** (repository + mock fallback, layering) — same concepts, reimplemented per runtime.
- Pure logic in TypeScript *can* be transpiled (`tsc --target ES5`) for lite reuse; anything touching
  the framework (ArkUI, decorators) must be rewritten.
