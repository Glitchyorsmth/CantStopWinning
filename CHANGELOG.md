# Changelog

All notable changes to CantStopWinning.

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
