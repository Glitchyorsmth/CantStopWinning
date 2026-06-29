package com.cantstopwinning;

import dev.anvil.api.Framework;
import dev.anvil.api.config.ConfigHandle;
import net.fabricmc.api.ClientModInitializer;

/**
 * CantStopWinning — rebuilt on the Anvil framework. Celebration/loss overlays fire from chat
 * triggers (and SkyHanni corpse-loss messages) via Anvil's overlay service.
 */
public final class CantStopWinningClient implements ClientModInitializer {

    /** The live settings handle — read with {@code CONFIG.get()}, persist with {@code CONFIG.save()}. */
    public static ConfigHandle<CswConfig> CONFIG;

    @Override
    public void onInitializeClient() {
        Framework.log().info("CantStopWinning loaded on Anvil {}",
                Framework.platform().minecraftVersion());

        CONFIG = Framework.config().register("cantstopwinning", CswConfig.class);
        CswAssets.extractDefaults();

        Framework.clientEvents().chatReceived(CswChat::onChat);
        CswTooltips.register();
        CswCommands.register();

        Framework.log().info("CSW ready: preset={} triggers={}",
                CONFIG.get().activePreset, CONFIG.get().activeTriggers().size());
    }
}
