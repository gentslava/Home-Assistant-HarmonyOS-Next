# 0003. Wear Engine P2P as the watch↔phone transport

- **Status:** Accepted
- **Date:** 2025-01-01

## Context

A Huawei watch typically reaches the home network only through the paired phone. Putting the Home
Assistant connection (base URL, long-lived token, TLS) directly on the watch is fragile and a
secret-management hazard on a constrained device. Home Assistant exposes a REST API that a phone
can reach on the local network.

## Decision

The watch talks to an Android **companion app** over **Wear Engine P2P** (`@kit.WearEngine`), and
the companion holds the HA REST connection. Messages are single UTF-8 JSON objects following a
versioned request/reply protocol (`v: 1`), correlated by `id`, with an 8s per-request timeout.
The companion is the source of truth for HA semantics; the watch is presentation-only.

The wire contract is specified in [../p2p-protocol.md](../p2p-protocol.md). The companion lives in
a **separate repository and is not written yet** — the protocol doc is the shared contract for both
sides.

## Consequences

- Secrets (HA URL/token) live on the phone, not the watch.
- The watch stays thin: it renders `EntityCard`s and echoes `EntityAction`s; adding an HA domain is
  mostly a companion + UI-mapping change.
- Two repos must evolve in lockstep on protocol changes — bump `v` and update both sides together.
- Real-device P2P requires the companion's `bundleName` + signing `fingerprint` via
  `WearEngineP2pClient.setRemoteApp(...)`; these are **configured** in `Services.ets` (real
  companion bundle + debug-cert fingerprint). It additionally needs Wear Engine approved in AGC.
- Hard dependency on Huawei Wear Engine + a paired Huawei phone; no standalone watch operation.

## Alternatives considered

- **Direct REST from the watch** — rejected: unreliable connectivity, secrets on-device, TLS pain.
- **HA Cloud / external URL from the watch** — deferred: viable later as an additional repository
  (see [ADR-0002](0002-repository-pattern-mock-fallback.md)), not the MVP path.
