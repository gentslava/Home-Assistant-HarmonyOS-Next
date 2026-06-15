# Home Assistant integration notes

Authoritative HA API facts for building the **companion** (and for mapping onto our
[P2P protocol](p2p-protocol.md)). Sourced from developers.home-assistant.io and
home-assistant.io. Don't invent fields — what's here is verified; gaps are flagged.

## Auth

- **Long-lived access token** (recommended for the companion — it's the user's own trusted app).
  User creates it in HA → Profile → "Long-Lived Access Tokens" (`http://<host>:8123/profile`).
  Shown once, ~10-year lifespan, no refresh flow.
- Header on every request: `Authorization: Bearer <TOKEN>`.
- **Validity check:** `GET /api/` → `200 {"message": "API running."}`; `401` = bad/expired token.
- On 401: ask the user to issue a new token. (OAuth2/IndieAuth exists but is overkill for a personal
  companion — only needed for a publicly distributed app.)

## Hosts

- Local: `http://homeassistant.local:8123` (mDNS) or `http://<IP>:8123`.
- Remote (Nabu Casa): `https://<id>.ui.nabu.casa` — same REST/WS API, same Bearer token, `wss://`.
- CORS is irrelevant here — the companion is a native Android HTTP client, not a browser.

## REST — read state

- `GET /api/states` → **array** of state objects (all entities; can be hundreds of KB).
- `GET /api/states/<entity_id>` → one object; `404` if missing.
- State object shape:
  ```json
  {
    "entity_id": "light.kitchen",
    "state": "on",
    "attributes": { "friendly_name": "Kitchen", "brightness": 180, "supported_color_modes": ["brightness"] },
    "last_changed": "2018-08-17T13:46:59.129248+00:00",
    "last_updated": "2018-08-17T13:46:59.129248+00:00",
    "context": { "id": "...", "user_id": null }
  }
  ```
- `last_changed` = when `state` changed; `last_updated` = any change incl. attributes.
- **No icon in REST** — the MDI icon is computed by the HA frontend. The companion picks an icon
  hint by `domain` (+ `device_class` for switch). Do not expect `EntityCard.icon` from HA.

## REST — call services (actions)

- `POST /api/services/<domain>/<service>`, JSON body = `service_data`.
- **For REST, put `entity_id` flat in the body** — `{ "entity_id": "light.kitchen" }` — works on all
  versions (the newer `target: { entity_id }` form is for YAML/WS; REST accepts entity_id in data).
- Returns an array of changed state objects (can be used for optimistic update, or ignored).
- Numeric params may be sent as strings (HA's own WS example uses `"brightness": "101"`) — so our
  `EntityAction.data: Record<string,string>` is compatible as-is.

## Supported domains

| Domain | States | Services |
|--------|--------|----------|
| `light` | `on` / `off` / `unavailable` / `unknown` | `turn_on` (brightness 1..255, brightness_pct 0..100, rgb_color, color_temp_kelvin, transition), `turn_off`, `toggle` |
| `switch` | `on` / `off` / `unavailable` / `unknown` | `turn_on`, `turn_off`, `toggle` (only param: entity_id) |
| `lock` | `locked` / `unlocked` / `locking` / `unlocking` / `jammed` / `open` / `unavailable` / `unknown` | `lock`, `unlock`, `open` (unlatch; only if supported; optional `code`) |
| `cover` | `open` / `closed` / `opening` / `closing` / `unavailable` | `open_cover`, `close_cover`, `stop_cover` (Phase 4: no position slider; `current_position` not used) |
| `scene` | a last-activated timestamp (treated as stateless) | `turn_on` (activate) |
| `sensor` | a numeric/text value (read-only) | none — no service call |

`light.turn_on`: **don't** send `brightness` and `brightness_pct` together. HA silently ignores
params the device doesn't support.

## Mapping HA → our P2P types

`EntityCard` (from [p2p-protocol.md](p2p-protocol.md)):
```
entity_id ← state.entity_id
domain    ← entity_id.split('.')[0]
name      ← attributes.friendly_name  (companion localizes; fallback object_id)
state     ← state.state               (pass through, incl. unavailable/unknown)
icon      ← companion chooses by domain/device_class (HA has none in REST)
primary/secondary ← EntityActions, built per domain below
```

Build `primary` per domain (all `kind:'SERVICE'`, `data` ≥ `{ entity_id }`):
- **light:** `toggle`; optional secondary `turn_on` with `brightness_pct`.
- **switch:** `toggle`.
- **lock:** state-dependent — `locked` → `unlock`, `unlocked` → `lock`; secondary `open` if supported.
- **cover:** state-dependent — `open` → `close_cover`, `closed` → `open_cover`, `opening`/`closing` → `stop_cover`; secondary = all three (`open_cover`/`close_cover`/`stop_cover`).
- **scene:** `turn_on` ("Activate"); no secondary. `state` is masked to the token `"scene"` (HA's raw state is a timestamp).
- **sensor:** **no `primary`** (read-only) — omit it; fold `attributes.unit_of_measurement` into `state` (`"21.5 °C"`); no secondary.

`CALL_SERVICE` → REST is a direct pass-through (no transform):
```
P2P  CALL_SERVICE { domain:"light", service:"toggle", data:{ entity_id:"light.kitchen" } }
HTTP POST /api/services/light/toggle   body { "entity_id": "light.kitchen" }
P2P  ACK { ok:true }   (or { ok:false, error } on 4xx)
```

Error mapping → `ACK.error`: 401 → "HA returned 401" (re-auth); 400 → body text (bad service data);
404 (single entity) → `SYNC_ENTITY_RESPONSE` with no `card`. `unavailable`/`unknown`: pass into
`EntityCard.state`; render as-is, don't build an actionable `primary` for them.

## Freshness strategy on the companion

- **MVP:** companion answers `SYNC_REQUEST` by calling `GET /api/states` (filter to light/switch/lock),
  and `CALL_SERVICE` via `POST /api/services/...`. Simple, fits the 8s P2P timeout.
- **Better (later):** companion holds one WebSocket to HA (`ws://<host>:8123/api/websocket`,
  flow `auth_required → auth → auth_ok`, then `subscribe_entities` for diff updates + `get_states`
  on start) and a local cache, so watch requests are served instantly from cache. This removes
  per-request polling. Live push to the watch would need a protocol `v` bump (companion → unsolicited
  messages, which v1 forbids).

## Verified gaps (don't assume)

- `lock` attributes (`code_format`/`changed_by`/`supported_features`) exist at the core level but the
  integration page doesn't show a verbatim JSON example — check on the real device before relying on them.
- `light.turn_on` rgbw/rgbww/white params appear as attributes in dev-docs but weren't in the action
  page's extracted text — verify per HA version before using in secondary actions.

## Sources

REST https://developers.home-assistant.io/docs/api/rest/ · WebSocket https://developers.home-assistant.io/docs/api/websocket/ ·
Auth https://developers.home-assistant.io/docs/auth_api/ · Frontend data https://developers.home-assistant.io/docs/frontend/data/ ·
Light https://developers.home-assistant.io/docs/core/entity/light/ · light.turn_on https://www.home-assistant.io/actions/light.turn_on/ ·
Lock https://www.home-assistant.io/integrations/lock/ · Switch https://www.home-assistant.io/integrations/switch/
