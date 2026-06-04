package com.cantstopwinning;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CantStopWinningClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CantStopWinning");
    public static ModConfig CONFIG;
    public static final CelebrationOverlay OVERLAY = new CelebrationOverlay();
    public static final CelebrationAudio AUDIO = new CelebrationAudio();

    @Override
    public void onInitializeClient() {
        CONFIG = ModConfig.load();
        HudRenderCallback.EVENT.register(OVERLAY);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                CswCommand.register(dispatcher));
        LOGGER.info("CantStopWinning loaded");
    }

    public static void triggerCelebration() {
        if (!CONFIG.enabled) return;
        OVERLAY.start();
        try {
            // Load from config folder first, fall back to JAR
            InputStream stream = null;
            Path externalWav = ModConfig.ASSETS_DIR.resolve("celebration.wav");
            if (Files.exists(externalWav)) {
                stream = Files.newInputStream(externalWav);
            } else {
                stream = CantStopWinningClient.class.getResourceAsStream("/assets/cantstopwinning/celebration.wav");
            }
            if (stream != null) {
                AUDIO.play(stream, () -> {
                    // Audio finished — stop overlay too
                    OVERLAY.stop();
                });
            } else {
                LOGGER.warn("No celebration audio found");
                // No audio — overlay will self-stop after 2 GIF loops
            }
        } catch (Exception e) {
            LOGGER.error("Error starting celebration", e);
        }
    }
}
