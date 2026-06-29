# CantStopWinning

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green?logo=mojangstudios)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric_Loader-0.18+-blue)](https://fabricmc.net/)
[![License](https://img.shields.io/github/license/Glitchyorsmth/CantStopWinning)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/Glitchyorsmth/CantStopWinning?include_prereleases)](https://github.com/Glitchyorsmth/CantStopWinning/releases)

I made this mod because I was mass grinding Diana and kept missing my own drops. I'd be zoned out auto-piloting burrows and not even notice when something good actually happened. So now when a drop hits, my entire screen explodes with falling money and slot machine sounds so there's zero chance I miss it again.

For the most dedicated SkyBlock gamblers who need the dopamine hit to match the grind.

A client-side Fabric mod that plays a fullscreen celebration (animated GIF + audio) when configurable chat messages appear. Set your triggers, keep grinding, and let the mod handle the hype.

<p>
<img src="demo.gif" width="48%" alt="Demo"> <img src="demo2.gif" width="48%" alt="Demo 2">
</p>

---

## Features

### Celebration & Loss Overlays
- Fullscreen image/GIF overlay on the HUD with synced audio
- Separate **win** and **loss** overlays — swap in your own (PNG / JPG / GIF + WAV)
- Volume and opacity controls

### Trigger Presets
- Named, switchable sets of triggers (e.g. *Diana*, *Fishing*) — only the active preset fires
- Create, rename, delete presets from a dropdown
- Each trigger is its own editable box; paginated 6 per page
- **Smart filtering** ignores player chat and economy spam (auction/bazaar/BIN/RNG meter) so only real drops trigger

### Corpse-Loss Detection (Glacite Tunnels)
- Reads SkyHanni corpse-profit messages to fire a loss overlay when you take an L
- **Set amount** mode (fires when the loss ≥ a coins amount you pick) or **Percentage** mode (loss exceeds a % of the key cost)
- Auto-fills Umber / Tungsten / Skeleton key prices from Bazaar buys and item tooltips (also editable by hand)

### Settings Screen
- Dark, scalable in-game GUI with four tabs: **General · Triggers · Corpse · Testing**
- Tooltips on every control
- **Testing tab** — one-click buttons fire the overlays or simulate a chat line through the detector

---

## Commands

| Command | Description |
|---|---|
| `/csw` or `/csw config` | Open the settings screen |
| `/csw on` / `/csw off` | Enable / disable the mod |
| `/csw test` / `/csw testloss` | Fire the win / loss overlay |
| `/csw sim <message>` | Run any text through the trigger/corpse/filter detector |
| `/csw help` | List all commands |

All commands are **client-side**.

---

## Installation

1. Download the latest `cantstopwinning-1.21.11-x.y.z.jar` from [Releases](https://github.com/Glitchyorsmth/CantStopWinning/releases)
2. Drop it into your `mods/` folder
3. Launch the game — the default trigger is ready to go

Requires **Fabric Loader 0.18+** and **Fabric API** for Minecraft **1.21.11**. The mod's framework
(Anvil) is bundled inside the jar — no separate download.

---

## Customization

Swap the overlay files in your config folder — no rebuilding needed:

```
config/
├── cantstopwinning.json          ← settings (edited in-game)
└── cantstopwinning/
    ├── celebration.gif / .wav    ← win overlay (PNG/JPG/GIF + WAV)
    └── loss.gif / .png / .wav    ← loss overlay
```

Defaults are extracted on first launch. Replace any file with your own (PNG > JPG > JPEG > GIF load priority) and restart.

---

## Built on Anvil

CantStopWinning runs on **Anvil**, a multi-version client-side Fabric framework. Same mappings build
targets current and future Minecraft, so the mod is ready to follow SkyBlock forward.

---

## License

This project is licensed under the [MIT License](LICENSE).
