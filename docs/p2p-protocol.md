# Wear Engine P2P Protocol (Watch ↔ Phone)

**Status:** active contract · **Protocol version:** `v: 1`

This is the wire contract between the watch app (this repo) and the Android **companion**
app (separate repo, not yet written). Both sides MUST conform to this document. Changing a
message shape is a breaking change — bump `v` and update both sides together
(see [ADR-0003](adr/0003-wear-engine-p2p-transport.md)).

The authoritative ArkTS definitions are in
[`P2pMessages.ets`](../apps/watch-arkts/entry/src/main/ets/domain/model/P2pMessages.ets) and
[`EntityCard.ets`](../apps/watch-arkts/entry/src/main/ets/domain/model/EntityCard.ets). This doc must stay in
sync with them; if they diverge, the `.ets` files win and this doc is a bug.

## Transport

- **Channel:** Wear Engine P2P messaging (`@kit.WearEngine`).
- **Encoding:** each message is a single UTF-8 JSON string (one JSON object per P2P message).
- **Peer pairing:** the watch resolves the first connected device and targets the companion by
  `remoteApp = { bundleName, fingerprint }`, configured via `WearEngineP2pClient.setRemoteApp(...)`
  in [`Services.ets`](../apps/watch-arkts/entry/src/main/ets/app/Services.ets). It is **configured**
  with the real companion bundle (`ru.gentslava.homeassistant.companion`) + its debug-cert
  fingerprint; real-device P2P additionally needs Wear Engine approved in AppGallery Connect.
- **Direction:** the watch is always the initiator. The companion only sends replies (it never
  pushes unsolicited messages in v1).

## Correlation & timeouts

Every message carries an `id`. The watch sends a request, parks a pending promise keyed by `id`,
and resolves it when a reply with the same `id` arrives. Implications for the companion:

- **Echo the request `id` verbatim** in the reply. A reply with a wrong/missing `id` is dropped.
- Replies may be returned **out of order**; the watch correlates by `id`, not by arrival order.
- The watch enforces an **8s timeout** per request
  ([`P2pHomeAssistantRepository`](../apps/watch-arkts/entry/src/main/ets/data/repository/P2pHomeAssistantRepository.ets)).
  After that the promise rejects and the reply is ignored even if it arrives late.
- During startup, `hasConnectedPeer()` and repo init are additionally bounded to **1200ms**
  before the app falls back to mock data.

## Envelope

Every message extends `BaseMsg`:

| Field | Type | Notes |
|-------|------|-------|
| `v` | `number` | Protocol version. Always `1` today. |
| `id` | `string` | Correlation id (watch-generated, e.g. `sync-…`, `svc-…`). Echo it back. |
| `type` | `MsgType` | One of the message types below. |

`MsgType = 'SYNC_REQUEST' | 'SYNC_RESPONSE' | 'SYNC_ENTITY_REQUEST' | 'SYNC_ENTITY_RESPONSE' | 'CALL_SERVICE' | 'ACK'`

## Messages

### `SYNC_REQUEST` → `SYNC_RESPONSE`
Fetch all entity cards (full list refresh).

Watch sends:
```json
{ "v": 1, "id": "sync-ab12", "type": "SYNC_REQUEST" }
```
Companion replies:
```json
{ "v": 1, "id": "sync-ab12", "type": "SYNC_RESPONSE", "cards": [ /* EntityCard[] */ ] }
```
If `cards` is absent, the watch treats it as an empty list.

### `SYNC_ENTITY_REQUEST` → `SYNC_ENTITY_RESPONSE`
Refresh a single entity after an action or pull.

Watch sends:
```json
{ "v": 1, "id": "get_card-cd34", "type": "SYNC_ENTITY_REQUEST", "entity_id": "light.kitchen" }
```
Companion replies:
```json
{ "v": 1, "id": "get_card-cd34", "type": "SYNC_ENTITY_RESPONSE", "card": { /* EntityCard | omitted */ } }
```
Omit `card` (or send it absent) if the entity no longer exists — the watch keeps the old card.

### `CALL_SERVICE` → `ACK`
Invoke a Home Assistant service.

Watch sends:
```json
{ "v": 1, "id": "svc-ef56", "type": "CALL_SERVICE",
  "domain": "light", "service": "toggle", "data": { "entity_id": "light.kitchen" } }
```
Companion replies:
```json
{ "v": 1, "id": "svc-ef56", "type": "ACK", "ok": true }
```
On failure:
```json
{ "v": 1, "id": "svc-ef56", "type": "ACK", "ok": false, "error": "HA returned 401" }
```
When `ok` is `false`, the watch throws `error` (or a generic message) and surfaces it via
`store.lastError`.

### `ACK` as a failure reply to sync requests
The companion may answer a `SYNC_REQUEST` / `SYNC_ENTITY_REQUEST` with an `ACK { ok: false }`
to signal an error. The watch interprets that as "empty list" / "no card" respectively rather
than crashing.

## Payload types

### `EntityCard`
```ts
interface EntityCard {
  entity_id: string;          // e.g. "light.kitchen"
  domain: string;             // "light"|"switch"|"lock"|"cover"|"scene"|"sensor"
  name: string;               // display name (companion may localize)
  state: string;              // "on"/"off", "locked"/"unlocked", "21.5 °C", …
  icon?: string;              // optional icon hint
  primary?: EntityAction;     // main tap action; OMITTED for read-only cards (sensor)
  secondary: EntityAction[];  // extra actions in the details screen
}
```

`primary` is optional and additive (no `v` bump): senders that always include it stay valid, and
readers must tolerate its absence — a card without `primary` renders no main action tile (e.g. a
read-only `sensor`). For `sensor`, the companion folds the unit into `state` (`"21.5 °C"`); for
`scene`, `state` is a stable token (`"scene"`), not HA's last-activated timestamp.

### `EntityAction`
A self-describing HA service call. The watch renders `label` and, on tap, sends `domain` +
`service` + `data` back as a `CALL_SERVICE`. The watch does **not** interpret HA semantics —
the companion decides which actions exist.
```ts
interface EntityAction {
  kind: 'SERVICE';            // only kind today
  label: string;             // UI text (companion-localized)
  domain: string;            // HA domain, e.g. "light"
  service: string;           // HA service, e.g. "toggle" / "turn_on" / "lock"
  data: Record<string, string>;  // at minimum { entity_id }
}
```

## Companion implementer checklist

- [ ] Listen for P2P messages, parse one JSON object per message.
- [ ] Echo `id` and `v` on every reply.
- [ ] Map `SYNC_REQUEST` → HA states → `EntityCard[]`, build `primary`/`secondary` actions.
- [ ] Map `CALL_SERVICE` → HA REST service call → `ACK { ok }` (with `error` on failure).
- [ ] Reply within 8s; prefer fast partial data over a slow complete reply.
- [ ] Localize `name`/`label` (the watch renders them as-is).
- [ ] Reuse `entity_id`s consistently so single-entity refresh and list refresh agree.
