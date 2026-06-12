# CantStopWinning — Progression Report

**Repo:** [github.com/Glitchyorsmth/CantStopWinning](https://github.com/Glitchyorsmth/CantStopWinning)
**Platform:** Fabric 0.18.1 · Minecraft 1.21.11 · Client-side only
**Status:** Released (v1.0.0) — code is ahead of release JAR by 3 commits

---

## What Was Built

A client-side Fabric mod that plays a fullscreen celebration (animated GIF + slot machine audio) when configurable Hypixel SkyBlock drop messages appear in chat.

**8 source files** from scratch:

| File | Purpose |
|---|---|
| `CantStopWinningClient.java` | Mod entrypoint, wires everything together |
| `CelebrationOverlay.java` | HUD renderer — decodes GIF frames into `NativeImageBackedTexture`, draws fullscreen, 2-loop limit |
| `CelebrationAudio.java` | WAV playback via `javax.sound.sampled`, volume control (linear→dB), stale-callback guard |
| `GifDecoder.java` | Animated GIF frame extraction with per-frame delay metadata |
| `ModConfig.java` | JSON config in `.minecraft/config/cantstopwinning/`, auto-extracts default assets on first launch |
| `ConfigScreen.java` | In-game GUI — volume slider, paginated trigger list (12/page), bordered rows, add/remove |
| `CswCommand.java` | Brigadier commands: `/csw on\|off\|test\|config\|help` |
| `ChatHudMixin.java` | Mixin on `ChatHud.addMessage` — trigger matching with server-only filtering |

---

## Features Delivered

- Fullscreen animated GIF overlay (transparent backgrounds supported)
- Synced audio playback (6-second trimmed WAV, fade-in/out)
- GIF limited to 2 loops, then auto-stops
- Configurable trigger messages (add/remove in-game)
- Volume slider (0–100%, default 50%)
- Paginated trigger list with `<` `>` navigation
- Bordered trigger rows in config GUI
- Server-only message filtering (ignores player chat across all Hypixel formats)
- Bazaar/Auction/BIN purchase message filtering
- Sell/listing/claim message filtering
- Color code stripping (`§x`) before matching
- Assets load from config folder — users swap GIF/WAV without rebuilding
- Default assets auto-extracted on first launch
- GitHub repo with clean single-commit history, README, demo GIFs, and release

---

## Bugs Fixed Along the Way

| Bug | Root Cause | Fix |
|---|---|---|
| Config screen crash ("Can only blur once per frame") | Double `renderBackground()` call | Removed manual call; `super.render()` handles it |
| GIF colors blue-tinted | Manual R↔B channel swap on top of internal ARGB→ABGR conversion | Pass `getRGB()` directly to `setColorArgb()` |
| GIF stretched/distorted | `drawTexture` used screen dimensions for texture size params | Used actual frame dimensions |
| All text invisible in config GUI | Colors like `0xFFFFFF` missing alpha byte = fully transparent | Prefixed all with `0xFF` alpha |
| Triggered by own chat messages | Player chat regex too narrow for Hypixel rank formats | Broadened to `\w{1,16}: ` + color code stripping |
| Triggered by Bazaar purchases | `[Bazaar] Bought 1x Skeleton Key` matched "Skeleton Key" trigger | Added `[Bazaar]`, `[Auction]`, `[BIN]` prefix filter |
| Triggered by sell messages | `You sold Skeleton Key x1` matched trigger | Added `You sold`, `Sold`, `Listing`, `Claimed`, `Collecting` filter |
| Audio "ZipFile invalid LOC header" | Stale JAR deployed before config folder changes | Clean rebuild + redeploy |
| Gradle download failed | Gradle 9.2 zip corrupted in cache | Switched to Gradle 9.4.1 |

---

## Asset Pipeline

- **celebration.wav** — MP3→WAV converted via Python + Windows Media Foundation COM (no ffmpeg). Trimmed to best 6-second window by RMS loudness analysis. 50ms fade-in, 300ms fade-out applied.
- **celebration.gif** — Falling money GIF with transparency, loaded from config folder.
- **demo.gif / demo2.gif** — MP4 gameplay recordings converted to GIF via OpenCV + Pillow at 480px/10fps, auto-reduced to 360px if >10MB.

---

## Git History (clean)

```
705f4e8  Filter out sell, listing, and claim messages
2e8528b  Filter out Bazaar, Auction, and BIN purchase messages
6813be7  Side by side demo GIFs
6978284  Add second demo GIF
dbf2e15  Add demo GIF
59e3e7e  Initial release — CantStopWinning v1.0.0
```

---

## Outstanding Item

The **GitHub release JAR** (`v1.0.0`) was uploaded on June 4 before the Bazaar/sell filter commits (2e8528b, 705f4e8). The release artifact is **outdated** — anyone downloading from Releases won't have the sell/purchase message filtering. A rebuild and re-upload would bring it current.
