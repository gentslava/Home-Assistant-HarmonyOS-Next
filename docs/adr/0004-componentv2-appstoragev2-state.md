# 0004. ArkUI V2 state management only

- **Status:** Accepted
- **Date:** 2025-01-01

## Context

ArkUI ships two generations of state decorators: V1 (`@State`, `@Observed`, `@Prop`, `AppStorage`)
and V2 (`@ComponentV2`, `@ObservedV2`, `@Trace`, `@Local`, `@Param`, `AppStorageV2`). Mixing them in
one component tree produces subtle, hard-to-debug reactivity bugs. The app needs one shared,
observable store that survives navigation between pages.

## Decision

We use the **V2 family exclusively**. The global store `HomeAssistantStore` is `@ObservedV2` with
`@Trace` fields (`cards`, `isSyncing`, `lastError`), registered once via
`AppStorageV2.connect(...)` in `EntryAbility` and read across pages. Components are `@ComponentV2`;
inputs use `@Param`/`@Local`.

## Consequences

- One consistent reactivity model; the store is the single source of UI truth.
- Do **not** introduce V1 decorators (`@State`/`@Observed`/`@Prop`/`AppStorage`) — they must not be
  mixed with V2 in the same tree.
- Aligns with CONTRIBUTING's strict-ArkTS rules: keep `build()` UI-only, avoid passing functions via
  `@Param`, avoid `any`/`unknown`.
- V2 decorators are newer; some examples online use V1 and must be translated before reuse.

## Alternatives considered

- **V1 state management** — rejected: project standardized on V2; mixing causes reactivity bugs.
- **A third-party state library** — rejected: unnecessary for this scope; no production dependencies.
