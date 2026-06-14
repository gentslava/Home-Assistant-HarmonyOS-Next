# 0002. Repository interface + non-blocking Mock fallback

- **Status:** Accepted
- **Date:** 2025-01-01

## Context

The watch emulator has no paired phone, so Wear Engine P2P is unavailable during most development.
The app must remain fully usable in the emulator, and UI startup must never hang waiting on P2P
init or the network (a watch that shows a blank screen for seconds is unacceptable).

## Decision

We define a single `HomeAssistantRepository` interface (`sync` / `syncEntityCard` / `callAction`)
with two implementations:

- `P2pHomeAssistantRepository` — real, over Wear Engine P2P.
- `MockHomeAssistantRepository` — in-memory fixtures for the emulator.

`Services.initWithFallback` is launched **fire-and-forget** from `EntryAbility` (not awaited). It
tries the real repo bounded by a **1200ms** timeout (peer check + init); on any failure or timeout
it falls back to `MockHomeAssistantRepository`. UI loads first, data fills in after.

## Consequences

- Emulator development always works (lands on mock) with zero configuration.
- Startup is never blocked on P2P/network — satisfies CONTRIBUTING rule #2.
- **Every change must keep the mock path working.** New domains/actions need matching mock fixtures,
  or the emulator regresses silently.
- The 1200ms budget is a deliberate UX trade-off: on a real-but-slow link the app may fall back to
  mock instead of waiting. Tune in `Services`, do not remove the bound.

## Alternatives considered

- **Awaiting real init before `loadContent`** — rejected: blocks startup, blank screen on emulator.
- **Compile-time flag to pick repo** — rejected: can't auto-recover on a real watch with no peer yet;
  runtime fallback handles both environments from one build.
