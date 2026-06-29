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
        fire("celebration");
    }

    /** Fires the loss overlay + sound (no-op if the mod is disabled). */
    public static void loss() {
        fire("loss");
    }

    private static void fire(String base) {
        CswConfig cfg = CantStopWinningClient.CONFIG.get();
        if (!cfg.enabled) {
            return;
        }
        Framework.overlay().play(OverlayRequest.of(
                CswAssets.image(base), CswAssets.sound(base),
                (float) cfg.overlayOpacity, (float) cfg.volume));
    }
}
