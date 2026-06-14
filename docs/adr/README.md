# Architecture Decision Records

ADRs capture **decisions that already shaped the code and why** — so neither a human nor an
AI agent re-litigates a settled question or writes code that contradicts a deliberate choice.

Read these before proposing structural changes. If your change reverses one of these decisions,
that is fine — but write a new ADR that supersedes it rather than silently diverging.

## Index

| # | Decision | Status |
|---|----------|--------|
| [0001](0001-clean-architecture-layering.md) | Clean Architecture layering (domain ← data, presentation → domain) | Accepted |
| [0002](0002-repository-pattern-mock-fallback.md) | Repository interface + non-blocking Mock fallback | Accepted |
| [0003](0003-wear-engine-p2p-transport.md) | Wear Engine P2P as the watch↔phone transport | Accepted |
| [0004](0004-componentv2-appstoragev2-state.md) | ArkUI V2 state management only (`@ComponentV2`/`AppStorageV2`) | Accepted |
| [0005](0005-target-full-wearable-not-lite.md) | Target full wearable (Watch 4/5/Ultimate), not lite wearable (GT) | Accepted |

## Conventions

- One decision per file: `NNNN-kebab-title.md`, numbered sequentially from `0001`.
- Start from [`template.md`](template.md).
- Status: `Proposed` → `Accepted` → (`Superseded by NNNN` / `Deprecated`).
- Keep them short. An ADR records context + decision + consequences, not a tutorial.
- ADRs are immutable once `Accepted`: to change a decision, add a new ADR and mark the old one
  `Superseded by NNNN`.
