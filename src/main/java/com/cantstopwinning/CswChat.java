package com.cantstopwinning;

import dev.anvil.api.util.Texts;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watches incoming chat and fires the win/loss overlays. Ported faithfully from the old
 * {@code ChatHudMixin}: corpse-loss detection, Bazaar key-price auto-update, an economy/player-chat
 * filter, and finally the active preset's trigger phrases.
 */
public final class CswChat {

    // Any player chat message on Hypixel ("Name: ...").
    private static final Pattern PLAYER_CHAT = Pattern.compile("(?:^|\\s)\\w{1,16}: ");

    private static final String[] ECONOMY_KEYWORDS = {
            "auction", "bazaar", "bought", "purchased", "sold for",
            "coins from selling", "buy order", "sell order",
            "Listing ", "Claimed ", "Collecting", "You sold",
            "RNG Meter", "RNG METER"
    };
    private static final String[] ECONOMY_PREFIXES = {"[Bazaar", "[Auction", "[BIN", "[Bazaar Utils"};

    // [SkyHanni] Profit for Vanguard Corpse: -20.9M
    private static final Pattern CORPSE_PROFIT =
            Pattern.compile("\\[SkyHanni\\] Profit for (\\w+) Corpse: ([+-]?[\\d,.]+)([KMBkmb]?)");
    // [Bazaar] Bought 1x Skeleton Key for 31.7M coins!
    private static final Pattern KEY_PURCHASE =
            Pattern.compile("\\[Bazaar\\] Bought \\d+x (Skeleton Key|Umber Key|Tungsten Key) for ([\\d,.]+)([KMBkmb]?) coins");

    private CswChat() {
    }

    /** Entry point — register as {@code Framework.clientEvents().chatReceived(CswChat::onChat)}. */
    public static void onChat(Component message) {
        CswConfig cfg = CantStopWinningClient.CONFIG.get();
        if (!cfg.enabled) {
            return;
        }
        String stripped = Texts.stripCodes(message.getString());

        // Corpse-loss detection (SkyHanni).
        if (cfg.corpseDetectionEnabled) {
            Matcher m = CORPSE_PROFIT.matcher(stripped);
            if (m.find()) {
                double profit = parseAmount(m.group(2), m.group(3));
                if (shouldTriggerLoss(cfg, m.group(1), profit)) {
                    Celebrations.loss();
                }
                return; // corpse line — don't also check triggers
            }
        }

        // Auto-detect key prices from Bazaar purchases.
        Matcher key = KEY_PURCHASE.matcher(stripped);
        if (key.find()) {
            String type = keyNameToType(key.group(1));
            if (type != null) {
                updateKeyPrice(cfg, type, parseAmount(key.group(2), key.group(3)));
            }
            return;
        }

        // Skip player chat + economy/trade noise.
        if (PLAYER_CHAT.matcher(stripped).find()) {
            return;
        }
        String lower = stripped.toLowerCase();
        for (String kw : ECONOMY_KEYWORDS) {
            if (lower.contains(kw.toLowerCase())) {
                return;
            }
        }
        for (String prefix : ECONOMY_PREFIXES) {
            if (stripped.startsWith(prefix)) {
                return;
            }
        }

        // Active preset's triggers.
        for (String trigger : List.copyOf(cfg.activeTriggers())) {
            if (!trigger.isEmpty() && stripped.contains(trigger)) {
                Celebrations.win();
                break;
            }
        }
    }

    /** Updates a key's cost (millions) from a detected raw-coin price; saves if it changed &gt;1%. */
    public static void updateKeyPrice(CswConfig cfg, String keyType, double priceInCoins) {
        double millions = Math.round(priceInCoins / 100_000.0) / 10.0;
        double current = keyCost(cfg, keyType);
        if (current > 0 && Math.abs(millions - current) / current < 0.01) {
            return;
        }
        switch (keyType) {
            case "vanguard" -> cfg.vanguardKeyCost = millions;
            case "umber" -> cfg.umberKeyCost = millions;
            case "tungsten" -> cfg.tungstenKeyCost = millions;
            default -> {
                return;
            }
        }
        CantStopWinningClient.CONFIG.save();
    }

    static String keyNameToType(String keyName) {
        return switch (keyName) {
            case "Skeleton Key" -> "vanguard";
            case "Umber Key" -> "umber";
            case "Tungsten Key" -> "tungsten";
            default -> null;
        };
    }

    private static double parseAmount(String number, String suffix) {
        try {
            double val = Double.parseDouble(number.replace(",", ""));
            return switch (suffix.toUpperCase()) {
                case "K" -> val * 1_000;
                case "M" -> val * 1_000_000;
                case "B" -> val * 1_000_000_000;
                default -> val;
            };
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean shouldTriggerLoss(CswConfig cfg, String corpseType, double profit) {
        if (profit >= 0) {
            return false;
        }
        if (cfg.corpseMode == CorpseMode.SIMPLE) {
            // Fire when the loss is at least the user-set amount (millions). 0 = any loss.
            return profit <= -(cfg.simpleAmountMillions * 1_000_000);
        }
        double keyCostMillions = keyCost(cfg, corpseType.toLowerCase());
        if (keyCostMillions <= 0) {
            return true; // no key cost set — fall back to simple
        }
        double lossThreshold = -(cfg.corpseThreshold / 100.0) * (keyCostMillions * 1_000_000);
        return profit <= lossThreshold;
    }

    private static double keyCost(CswConfig cfg, String type) {
        return switch (type) {
            case "umber" -> cfg.umberKeyCost;
            case "tungsten" -> cfg.tungstenKeyCost;
            case "vanguard" -> cfg.vanguardKeyCost;
            default -> 0;
        };
    }
}
