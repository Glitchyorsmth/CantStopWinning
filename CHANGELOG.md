# Changelog

All notable changes to CantStopWinning.

## [1.3.0] — 2026-07-04

### Added
- **Preset composition** — a preset can include other presets by live reference (e.g. "General"
  includes "Slayer" and "Diana"): it fires on their triggers too, and editing an included preset
  updates everywhere it's included immediately, no copying or desync. Only presets with no
  includes of their own are eligible to be included. Manage it from the new **Includes** button on
  the Triggers tab.
- **Preset import/export** — copy a preset (its own triggers, plus everything it includes) to the
  clipboard as a shareable code from the Triggers tab, and paste one back in with Import. Importing
  a composite preset recreates its named sub-presets locally too, not just a flat trigger dump.
- **Import overwrite protection** — if importing would overwrite an existing preset with different
  content, you can either get a warning after it happens or a click-to-confirm prompt in chat
  before it happens (5 seconds to respond). Pick your preference on the General tab.
- **Key-saved corpse detection** — when Hypixel's Resourceful perk saves your key from being
  consumed, that corpse no longer counts as a loss (the key wasn't spent, so nothing was lost),
  with an option to also fire the celebration overlay instead. New "Key saved" control on the
  Corpse tab.

### Changed
- **"New" preset is easier to use** — it now always creates a genuinely fresh preset and puts its
  name in the box ready to rename, instead of silently doing nothing if the box still showed the
  previously-selected preset's name.
- **Text fields now behave normally** — clicking places the cursor where you clicked, and
  click-dragging selects text. Previously clicking only focused the field without moving the
  cursor, so editing meant arrowing over one character at a time.
- Every tooltip in the settings screen was shortened and simplified.
- Internal: retargeted the 26.x build from 26.1 to 26.2 (Mojang skipped 26.1) — no user-facing effect.

## [1.2.0] — 2026-06-29

Full rebuild on the **Anvil** framework, with a redesigned settings screen and trigger presets.
All detection and overlay behaviour from 1.1.1 is preserved.

### Added
- **Trigger presets** — named, switchable sets of triggers (e.g. "Diana", "Fishing"). Pick one via a
  dropdown; only the active preset's triggers fire. Create / rename / delete presets in-game.
- **Redesigned settings screen** — dark SkyFisher style, four tabs: **General** (toggle, volume,
  opacity), **Triggers** (presets + trigger boxes), **Corpse** (loss detection), **Testing**.
- **Editable triggers** — each trigger is now an editable box (change the text in place), with
  pagination (6 per page).
- **Testing tab** — one-click buttons that close the menu and fire the celebration/loss overlay or
  simulate a chat line through the detector, each with `[CSW]` chat feedback.
- **Corpse "Set amount" mode** — fire the loss overlay when a loss is at least a coin amount you set
  (alongside the existing Percentage mode).
- **Editable key prices** — set Umber / Tungsten / Skeleton prices manually (still auto-filled from
  Bazaar buys and tooltips).
- **Tooltips** on every control.
- **`/csw sim <message>`** — run any text through the trigger/corpse/filter detector for testing.
- Settings screen scales to fit any window size.

### Changed
- Rebuilt entirely on the Anvil modding framework (config, commands, chat, overlay, audio now run
  through the framework). Multi-version build (1.21.11 now; 26.x-ready).
- Settings are stored at `config/cantstopwinning.json` (single file; old per-folder JSON is no longer
  read — re-add custom triggers via the new presets UI).

### Notes
- Overlay assets still live in `config/cantstopwinning/` (celebration/loss GIF + WAV), swappable.
- Requires Fabric Loader 0.18+ and Fabric API for Minecraft 1.21.11. Anvil is bundled in the jar.

## [1.1.1] — 2026-06-19
- Multi-format overlays (PNG/JPG/JPEG/GIF, priority PNG > JPG > JPEG > GIF).
- Full-quality PNG loss overlay; GIF transparency fix (palette transparent index → alpha 0).
- Scale-to-fill rendering, centered; single-frame images stay until audio finishes.

## [1.1.0] — 2026-06-19
- Corpse-loss detection via SkyHanni messages (Simple / Percentage modes, auto key prices).
- Overlay opacity slider; custom loss image + audio.
- `/csw` opens config; `/csw testloss`; keyword-based economy filter.
- Tabbed config GUI (Triggers / Corpse).

## [1.0.0] — 2026-06-04
- Initial release: fullscreen celebration overlay (GIF + audio) on configurable chat triggers,
  server-only filtering, config GUI with volume slider + paginated trigger list, swappable assets,
  `/csw config|test|on|off|help`.

[1.2.0]: https://github.com/Glitchyorsmth/CantStopWinning/releases/tag/v1.2.0
[1.1.1]: https://github.com/Glitchyorsmth/CantStopWinning/releases/tag/v1.1.1
[1.1.0]: https://github.com/Glitchyorsmth/CantStopWinning/releases/tag/v1.1.0
[1.0.0]: https://github.com/Glitchyorsmth/CantStopWinning/releases/tag/v1.0.0
