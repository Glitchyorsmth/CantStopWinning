package com.cantstopwinning;

import dev.anvil.api.Framework;
import dev.anvil.api.overlay.OverlayRequest;

/**
 * Fires the win / loss overlays through Anvil's overlay service, using the current settings
 * (volume + opacity). Assets resolve from {@link CswAssets} (config folder, swappable).
 */
public final class Celebrations {

    private Celebrations() {
    }

    /** Fires the celebration overlay + sound (no-op if the mod is disabled). */
    public static void win() {
        CswConfig cfg = CantStopWinningClient.CONFIG.get();
        if (!cfg.enabled) {
            return;
        }
        Framework.overlay().play(OverlayRequest.of(
                CswAssets.image("celebration"), CswAssets.sound("celebration"),
                (float) cfg.overlayOpacity, (float) cfg.volume));
    }

    /** Fires the loss overlay + sound (no-op if the mod is disabled). */
    public static void loss() {
        CswConfig cfg = CantStopWinningClient.CONFIG.get();
        if (!cfg.enabled) {
            return;
        }
        Framework.overlay().play(OverlayRequest.of(
                CswAssets.image("loss"), CswAssets.sound("loss"),
                (float) cfg.overlayOpacity, (float) cfg.volume));
    }
}
