# 0005. Target full wearable (Watch), not lite wearable (GT)

- **Status:** Accepted
- **Date:** 2026-06-14

## Context

The project began aiming at the Huawei **Watch GT 6**, on the assumption it was a full HarmonyOS
wearable that runs ArkTS apps. Investigation plus an on-device test disproved this. For third-party
developers, the **Watch GT series (4/5/6) is a `lite wearable`**: FA model, JS limited to ES5.1,
HML/CSS/JS, JerryScript runtime, haptobin `.bin` packages. Full ArkTS (Stage Model, ArkUI, ARK
runtime) is available only on the **Watch 4/5/Ultimate** class.

This was verified, not assumed (full evidence in [platform-constraints.md](../platform-constraints.md)):
a correctly signed minimal ArkTS Stage HAP fails on GT 6 with `Failed to decompress` (signature
passed, format rejected), while a lite JS HAP installs and runs; a byte-level diff shows the Stage
build carries `PANDA` bytecode (`ark13.0.1.0`) and the lite build a haptobin `.bin`. Four bypass
paths (hack-install, sign-as-lite, mimic build, embed components) are all structurally blocked.

## Decision

This repository targets the **full wearable class — Huawei Watch 4/5/Ultimate** — and stays on
ArkTS + ArkUI + Stage Model. We do **not** attempt to run it on Watch GT.

A Home Assistant client for the GT series is a **separate lite-wearable track** (JS/HML/CSS,
project `HomeAssistantLite`), not a build variant of this code. The two tracks share the
[P2P protocol contract](../p2p-protocol.md), the domain model, and the architecture — by hand, not
by shared binary.

## Consequences

- The ArkTS code, `ArcList` round-screen UI, and `AppStorageV2` here are valid for Watch 4/5/Ultimate.
- GT-series users are served (if at all) by the separate lite track, with its hard limits
  (HAP ≤10 MB, page ≤48 KB, ES5.1, no background, no native `ArcList`).
- Native edge-scaling `ArcList` is only achievable on the full wearable; on lite it can at best be
  approximated on a canvas.
- Stop spending effort on GT→ArkTS bypasses — proven closed. Re-evaluate only on a concrete Huawei
  signal (see "Future outlook" in platform-constraints.md).
- Open question (not decided here): how the two tracks are organized — monorepo vs separate repos,
  and where the shared P2P contract lives.

## Alternatives considered

- **Target Watch GT with ArkTS** — rejected: physically impossible (device rejects the Stage format).
- **Rewrite this app as lite JS to fit GT** — rejected as the *primary* path: loses ArkTS/ArkUI and
  native round-screen UI. Pursued instead as a parallel track for GT coverage.
- **Bypass / hack ArkTS onto GT** — rejected: all four paths structurally blocked; brick risk.
