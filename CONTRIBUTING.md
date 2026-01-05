# Contributing

Thanks for helping improve this project!

## Project goals

- A stable, fast watch UI that works well on **round screens** (Arc components).
- Reliable watch ↔ phone communication via Wear Engine P2P.
- Easy extension to new Home Assistant entity domains.

## Repository layout

- `entry/src/main/ets/pages/` — pages (Index, EntityDetails, Settings)
- `entry/src/main/ets/presentation/` — UI components + store
- `entry/src/main/ets/domain/` — models + repository interfaces
- `entry/src/main/ets/data/` — P2P + repositories (real + mock)

## Development rules (important)

### 1) Emulator must work (Mock mode)
The watch emulator has **no paired phone**.
Any change must keep the app usable in emulator via `MockHomeAssistantRepository`.

### 2) Avoid blocking app startup
Never block UI startup on P2P init or network calls.
Use async init + fallback to mock.

### 3) ArkTS strict checks
This project uses strict ArkTS checks:
- avoid `any` / `unknown`
- avoid passing functions via `@Prop` / `@Param`
- prefer `@ComponentV2` patterns and keep `build()` “UI-only”
- avoid `const/let` inside `build()` if your linter flags it

### 4) Navigation
Prefer UIContext router (non-deprecated APIs) over deprecated signatures where possible.

### 5) Round-screen UI
Prefer `ArcList` / `ArcScrollBar` for scrollable content.
Test on:
- emulator
- real device (if available)

## Adding a new entity domain

1. Update domain → presentation mapping (icon/color/labels)
2. Add DTO messages for sync and service call
3. Implement phone-side REST call to HA
4. Add mock entities + actions for emulator testing
5. Add screenshots if UI changes

## Commit style

- Use clear messages: `feat:`, `fix:`, `refactor:`, `docs:`
- Keep PRs focused (one feature/fix per PR)

## Pull request checklist

- [ ] Runs on emulator (Mock mode)
- [ ] No new ArkTSCheck errors/warnings
- [ ] UI fits round screen (no clipped text/buttons)
- [ ] Sync/actions still work (or mock still works)
- [ ] README updated if needed (new feature/screenshots)
