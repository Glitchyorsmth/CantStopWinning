package com.cantstopwinning;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CantStopWinningClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CantStopWinning");
    public static ModConfig CONFIG;
    public static final CelebrationOverlay OVERLAY = new CelebrationOverlay("celebration.gif", "cel_frame");
    public static final CelebrationOverlay LOSS_OVERLAY = new CelebrationOverlay("loss.gif", "loss_frame");
    public static final CelebrationAudio AUDIO = new CelebrationAudio();
    public static final CelebrationAudio LOSS_AUDIO = new CelebrationAudio();

    // Pattern for SkyHanni Bazaar price in tooltips
    // "Bazaar Sell Order: 34,309,663.8 coins" (instant buy price)
    private static final Pattern BAZAAR_SELL_PATTERN = Pattern.compile(
            "Bazaar Sell Order: ([\\d,.]+) coins"
    );

    // Throttle tooltip price checks — no more than once every 5 seconds
    private static long lastTooltipCheck = 0;

    @Override
    public void onInitializeClient() {
        CONFIG = ModConfig.load();
        HudRenderCallback.EVENT.register(OVERLAY);
        HudRenderCallback.EVENT.register(LOSS_OVERLAY);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                CswCommand.register(dispatcher));

        // Intercept "/csw" with no args — Brigadier can't handle root execution in Fabric,
        // so we catch it here before it reaches the dispatcher
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            if (command.trim().equalsIgnoreCase("csw")) {
                CswCommand.openConfig();
                return false; // cancel — we handled it
            }
            return true; // let everything else through
        });

        // Auto-detect key prices from tooltips (SkyHanni adds Bazaar prices to item tooltips)
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (CONFIG == null || !CONFIG.corpseDetectionEnabled) return;
            if (!"percentage".equals(CONFIG.corpseMode)) return;

            long now = System.currentTimeMillis();
            if (now - lastTooltipCheck < 5000) return;

            String itemName = stack.getName().getString().replaceAll("§.", "");
            String keyType = null;
            if (itemName.contains("Skeleton Key")) keyType = "vanguard";
            else if (itemName.contains("Umber Key")) keyType = "umber";
            else if (itemName.contains("Tungsten Key")) keyType = "tungsten";

            if (keyType == null) return;
            lastTooltipCheck = now;

            for (Text line : lines) {
                String text = line.getString().replaceAll("§.", "");
                Matcher m = BAZAAR_SELL_PATTERN.matcher(text);
                if (m.find()) {
                    try {
                        double price = Double.parseDouble(m.group(1).replace(",", ""));
                        updateKeyPrice(keyType, price);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        });

        LOGGER.info("CantStopWinning loaded");
    }

    /**
     * Updates a key's cost from a detected price (in raw coins).
     * Only saves if the price changed meaningfully (>1%).
     */
    public static void updateKeyPrice(String keyType, double priceInCoins) {
        double priceInMillions = Math.round(priceInCoins / 100_000.0) / 10.0; // Round to 0.1M

        double current;
        switch (keyType) {
            case "vanguard": current = CONFIG.vanguardKeyCost; break;
            case "umber": current = CONFIG.umberKeyCost; break;
            case "tungsten": current = CONFIG.tungstenKeyCost; break;
            default: return;
        }

        // Only save if price changed by more than 1%
        if (current > 0 && Math.abs(priceInMillions - current) / current < 0.01) return;

        switch (keyType) {
            case "vanguard": CONFIG.vanguardKeyCost = priceInMillions; break;
            case "umber": CONFIG.umberKeyCost = priceInMillions; break;
            case "tungsten": CONFIG.tungstenKeyCost = priceInMillions; break;
        }
        CONFIG.save();
        LOGGER.info("Updated {} key cost to {}M", keyType, priceInMillions);
    }

    public static void triggerCelebration() {
        if (!CONFIG.enabled) return;
        // Stop any ongoing loss overlay
        LOSS_OVERLAY.stop();
        LOSS_AUDIO.stop();

        OVERLAY.start();
        try {
            InputStream stream = null;
            Path externalWav = ModConfig.ASSETS_DIR.resolve("celebration.wav");
            if (Files.exists(externalWav)) {
                stream = Files.newInputStream(externalWav);
            } else {
                stream = CantStopWinningClient.class.getResourceAsStream("/assets/cantstopwinning/celebration.wav");
            }
            if (stream != null) {
                AUDIO.play(stream, () -> {
                    OVERLAY.stop();
                });
            } else {
                LOGGER.warn("No celebration audio found");
            }
        } catch (Exception e) {
            LOGGER.error("Error starting celebration", e);
        }
    }

    public static void triggerLoss() {
        if (!CONFIG.enabled) return;
        // Note: corpseDetectionEnabled is checked by the caller (ChatHudMixin)
        // so /csw testloss still works even when corpse detection is toggled off
        // Stop any ongoing win overlay
        OVERLAY.stop();
        AUDIO.stop();

        LOSS_OVERLAY.start();
        try {
            InputStream stream = null;
            Path externalWav = ModConfig.ASSETS_DIR.resolve("loss.wav");
            if (Files.exists(externalWav)) {
                stream = Files.newInputStream(externalWav);
            } else {
                stream = CantStopWinningClient.class.getResourceAsStream("/assets/cantstopwinning/loss.wav");
            }
            if (stream != null) {
                LOSS_AUDIO.play(stream, () -> {
                    LOSS_OVERLAY.stop();
                });
            } else {
                LOGGER.warn("No loss audio found");
            }
        } catch (Exception e) {
            LOGGER.error("Error starting loss overlay", e);
        }
    }
}
