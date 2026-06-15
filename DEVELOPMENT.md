# Development workflow (AI-first)

How work happens in this repo when a human and AI agents build together. The goal is an
**AI-manageable** codebase: agents can enter context fast, follow a disciplined loop, and verify
their own work — with the human owning direction and final sign-off.

This complements [CONTRIBUTING.md](CONTRIBUTING.md) (contribution rules, PR checklist, "add a new HA
domain"). Operational facts live in [AGENTS.md](AGENTS.md); read that first.

## The AI artifacts (what each file is for)

| Artifact | Purpose | When to read / update |
|----------|---------|-----------------------|
| [AGENTS.md](AGENTS.md) | Router: shared context + map to per-app rules | Every session |
| `apps/<app>/AGENTS.md` | Platform-specific rules (ArkTS / lite-JS / Kotlin) | When editing that app |
| [CLAUDE.md](CLAUDE.md) | Thin Claude Code layer; imports `AGENTS.md` | Claude-specific guidance only |
| [docs/architecture.md](docs/architecture.md) | System map, layers, startup & data flow | Before cross-layer work |
| [docs/p2p-protocol.md](docs/p2p-protocol.md) | Watch↔phone wire contract (companion = separate, unwritten repo) | Before touching messaging; update on any change |
| [docs/adr/](docs/adr/) | Why decisions were made | Before structural changes; supersede, don't contradict |
| [.claude/agents/](.claude/agents/) | Role subagents (architect, reviewer, test-writer) | Delegate the matching phase |

Treat all of these like code: review them, prune them, and update them in the **same PR** as the
change they describe. Stale instructions are worse than none — they actively mislead agents.

## The loop: Explore → Plan → Code → Commit

1. **Explore (read-only).** Load the relevant context: `AGENTS.md`, the touched source, the matching
   ADRs, and `docs/p2p-protocol.md` if messaging is involved. Don't edit yet. Skipping this builds a
   plan on assumptions.
2. **Plan.** For anything beyond a one-line diff, produce a plan first — delegate to the
   **`architect`** subagent for features, protocol/contract changes, or a new HA domain across apps.
   Surface assumptions explicitly and get them confirmed. The human reviews the plan before code.
3. **Code.** Implement the smallest correct slice, following the **per-app `AGENTS.md`** for that
   platform (ArkTS layers/V2 · lite-JS ES5.1 · Kotlin). Keep the Mock path working. Write/adjust tests
   with the **`test-writer`** subagent (hypium / JUnit / on-device).
4. **Review & commit.** Run the **`code-reviewer`** subagent in a fresh context. Then commit with a
   Conventional Commit message, one focused change per PR.

## Human-in-the-loop checkpoints

The human owns decisions; agents own execution. Stop and get a human decision before:

- Reversing or adding an **ADR** (architecture, transport, protocol).
- A **breaking P2P protocol change** (`v` bump) — it commits the future companion repo to a contract.
- Filling in real secrets/identity: `setRemoteApp(...)` / `PEER_FINGERPRINT` / `client_id` /
  `supportLists` / `agconnect-services.json` (today intentional placeholders).
- Adding a **production dependency** to a watch app (they are intentionally dependency-free).
- Anything where the spec is ambiguous — the author prefers an extra question over a wrong inference.

## Verification (close the loop yourself)

A task is **not done** until it is verified. State the evidence; never claim success without it.

There is **no committed CLI build / CI** yet — verification is per-app (details in each `AGENTS.md`):

- **watch-arkts:** runs on the emulator in Mock mode (no round-screen clipping); Code Linter clean.
- **watch-lite:** builds and installs via DevEco Assistant; runs on a real GT (no previewer for P2P).
- **companion:** `./gradlew test` green; Connect & test lists real HA entities.
- The Mock path still works in each watch app.

If you cannot run a check in your environment, say so explicitly and hand the exact steps to the
human — do not assert a check passed when you didn't run it.

> Improvement worth doing: commit a `hvigorw` wrapper and add CI (lint + unit tests on push) so this
> loop closes automatically. Until then, treat the DevEco Studio steps above as the gate.

## Context hygiene

- `/clear` between unrelated tasks; don't carry a polluted context forward.
- After two failed attempts at the same fix, stop, `/clear`, and re-approach with a better prompt
  rather than piling on corrections.
- Prefer the role subagents for noisy/large explorations so the main context stays focused.
- If an agent keeps ignoring a rule, the instruction file is probably too long — tighten it.
