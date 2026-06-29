package com.cantstopwinning.mixin;

import com.cantstopwinning.CantStopWinningClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    // Matches any player chat message on Hypixel
    private static final Pattern PLAYER_CHAT_PATTERN = Pattern.compile(
            "(?:^|\\s)\\w{1,16}: "
    );

    // Economy/trade keywords — if a message contains ANY of these, it's not a drop
    private static final String[] ECONOMY_KEYWORDS = {
            "auction", "bazaar", "bought", "purchased", "sold for",
            "coins from selling", "buy order", "sell order",
            "Listing ", "Claimed ", "Collecting", "You sold",
            "RNG Meter", "RNG METER"
    };

    private static final String[] ECONOMY_PREFIXES = {
            "[Bazaar", "[Auction", "[BIN", "[Bazaar Utils"
    };

    // Matches SkyHanni corpse profit messages
    // Example: [SkyHanni] Profit for Vanguard Corpse: -20.9M
    private static final Pattern CORPSE_PROFIT_PATTERN = Pattern.compile(
            "\\[SkyHanni\\] Profit for (\\w+) Corpse: ([+-]?[\\d,.]+)([KMBkmb]?)"
    );

    // Matches Bazaar key purchases to auto-update key costs
    // Example: [Bazaar] Bought 1x Skeleton Key for 31.7M coins!
    private static final Pattern KEY_PURCHASE_PATTERN = Pattern.compile(
            "\\[Bazaar\\] Bought \\d+x (Skeleton Key|Umber Key|Tungsten Key) for ([\\d,.]+)([KMBkmb]?) coins"
    );

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (CantStopWinningClient.CONFIG == null || !CantStopWinningClient.CONFIG.enabled) return;

        String plain = message.getString();

        // Strip color codes (§x) that Hypixel uses
        String stripped = plain.replaceAll("§.", "");

        // --- Corpse profit detection (SkyHanni) ---
        if (CantStopWinningClient.CONFIG.corpseDetectionEnabled) {
            Matcher corpseMatcher = CORPSE_PROFIT_PATTERN.matcher(stripped);
            if (corpseMatcher.find()) {
                String corpseType = corpseMatcher.group(1);
                double profit = parseAmount(corpseMatcher.group(2), corpseMatcher.group(3));

                if (shouldTriggerLoss(corpseType, profit)) {
                    CantStopWinningClient.triggerLoss();
                }
                return; // Don't also check regular triggers for corpse messages
            }
        }

        // --- Auto-detect key prices from Bazaar purchases ---
        Matcher keyMatcher = KEY_PURCHASE_PATTERN.matcher(stripped);
        if (keyMatcher.find()) {
            String keyName = keyMatcher.group(1);
            double price = parseAmount(keyMatcher.group(2), keyMatcher.group(3));
            String keyType = keyNameToType(keyName);
            if (keyType != null) {
                CantStopWinningClient.updateKeyPrice(keyType, price);
            }
            return; // Bazaar purchase — don't check triggers
        }

        // Skip if this looks like a player chat message
        if (PLAYER_CHAT_PATTERN.matcher(stripped).find()) return;

        // Skip any message containing economy/trade keywords — only actual drops should trigger
        String lower = stripped.toLowerCase();
        for (String kw : ECONOMY_KEYWORDS) {
            if (lower.contains(kw.toLowerCase())) return;
        }
        for (String prefix : ECONOMY_PREFIXES) {
            if (stripped.startsWith(prefix)) return;
        }

        List<String> triggers = List.copyOf(CantStopWinningClient.CONFIG.triggerMessages);
        for (String trigger : triggers) {
            if (stripped.contains(trigger)) {
                CantStopWinningClient.triggerCelebration();
                break;
            }
        }
    }

    /**
     * Parses a shorthand amount like "-20.9M" into raw coins.
     */
    private static double parseAmount(String number, String suffix) {
        try {
            double val = Double.parseDouble(number.replace(",", ""));
            switch (suffix.toUpperCase()) {
                case "K": val *= 1_000; break;
                case "M": val *= 1_000_000; break;
                case "B": val *= 1_000_000_000; break;
            }
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Converts a key item name to a corpse type.
     */
    private static String keyNameToType(String keyName) {
        switch (keyName) {
            case "Skeleton Key": return "vanguard";
            case "Umber Key": return "umber";
            case "Tungsten Key": return "tungsten";
            default: return null;
        }
    }

    /**
     * Decides whether a corpse profit value should trigger the loss overlay.
     */
    private static boolean shouldTriggerLoss(String corpseType, double profit) {
        if (profit >= 0) return false;

        String mode = CantStopWinningClient.CONFIG.corpseMode;
        if ("simple".equals(mode)) {
            return true;
        }

        // Percentage mode
        double keyCostMillions = getKeyCost(corpseType);
        if (keyCostMillions <= 0) return true; // No key cost set, fall back to simple

        double keyCostCoins = keyCostMillions * 1_000_000;
        double threshold = CantStopWinningClient.CONFIG.corpseThreshold;
        double lossThreshold = -(threshold / 100.0) * keyCostCoins;

        return profit <= lossThreshold;
    }

    private static double getKeyCost(String corpseType) {
        switch (corpseType.toLowerCase()) {
            case "umber": return CantStopWinningClient.CONFIG.umberKeyCost;
            case "tungsten": return CantStopWinningClient.CONFIG.tungstenKeyCost;
            case "vanguard": return CantStopWinningClient.CONFIG.vanguardKeyCost;
            default: return 0;
        }
    }
}
