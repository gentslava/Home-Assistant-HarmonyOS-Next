# 0001. Clean Architecture layering

- **Status:** Accepted
- **Date:** 2025-01-01

## Context

The watch app must support two interchangeable data backends (real P2P and a mock for the
emulator) and is expected to grow new Home Assistant domains and, later, new transports
(cloud / direct REST). UI churn on a round screen is high. We need the business core to stay
stable while adapters and UI change.

## Decision

We organize `entry/src/main/ets/` into layers with dependencies pointing inward:

- `domain/` — pure ArkTS models and the `HomeAssistantRepository` interface. No imports from
  `data/`, `presentation/`, or HarmonyOS kits.
- `data/` — adapters that implement `domain` interfaces and own all Wear Engine / encoding details.
- `presentation/` — pages, the global store, and UI components; depends only on `domain`.
- `app/` — composition root (`Services`) that wires a concrete repository to the interface.
- `core/` — framework-agnostic helpers (json, log, uuid).

## Consequences

- The `HomeAssistantRepository` interface is the seam: swapping or adding a backend touches only
  `data/` and `Services`, never `domain/` or `presentation/`.
- `domain/` stays unit-testable without ArkUI or Wear Engine.
- Cost: more indirection and files than a flat structure — deliberate, to absorb backend/UI change.
- Enforcement is by review (no module boundary tooling). Reviewers must reject `presentation → data`
  or framework imports leaking into `domain/`.

## Alternatives considered

- **Flat `pages/` + ad-hoc services** — rejected: couples UI to transport, blocks the mock fallback.
- **MVVM without a domain layer** — rejected: no clean seam for multiple/future backends.
