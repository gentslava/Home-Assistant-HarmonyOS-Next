# Phone companion (Android) — placeholder

**Status: not written yet.** This directory reserves the spot for the Android companion app in the
[monorepo](../../docs/adr/0006-monorepo-structure.md).

## Role

The watch apps have no direct network path to Home Assistant. The companion is the bridge:

```
Watch (ArkTS or lite-JS) ──Wear Engine P2P──▶ Phone companion (this) ──HA REST──▶ Home Assistant
```

It pairs with the watch over **Wear Engine P2P** (HiWear SDK on Android), holds the Home Assistant
connection (base URL + long-lived token), translates P2P messages into HA REST calls, and sends
state back.

## Contract

Build against the shared, versioned wire contract:
**[../../docs/p2p-protocol.md](../../docs/p2p-protocol.md)** (`v:1`). Both watch apps already speak it.

A companion implementer checklist is in that document. Any protocol change is breaking — bump `v`
and update both sides.

## Planned stack

Kotlin + Jetpack Compose. Will live here as `apps/phone-android/` (open in Android Studio).
