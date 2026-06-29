package com.cantstopwinning;

import dev.anvil.api.util.Texts;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads dungeon-key Bazaar prices off item tooltips (added by SkyHanni) to auto-fill the key costs
 * used by percentage-mode corpse detection. Ported from the old client's tooltip hook. Throttled.
 */
public final class CswTooltips {

    // "Bazaar Sell Order: 34,309,663.8 coins"
    private static final Pattern BAZAAR_SELL = Pattern.compile("Bazaar Sell Order: ([\\d,.]+) coins");

    private static long lastCheck = 0;

    private CswTooltips() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register(CswTooltips::onTooltip);
    }

    private static void onTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        CswConfig cfg = CantStopWinningClient.CONFIG.get();
        if (!cfg.corpseDetectionEnabled || cfg.corpseMode != CorpseMode.PERCENTAGE) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCheck < 5000) {
            return;
        }

        String name = Texts.stripCodes(stack.getHoverName().getString());
        String keyType;
        if (name.contains("Skeleton Key")) {
            keyType = "vanguard";
        } else if (name.contains("Umber Key")) {
            keyType = "umber";
        } else if (name.contains("Tungsten Key")) {
            keyType = "tungsten";
        } else {
            return;
        }
        lastCheck = now;

        for (Component line : lines) {
            Matcher m = BAZAAR_SELL.matcher(Texts.stripCodes(line.getString()));
            if (m.find()) {
                try {
                    CswChat.updateKeyPrice(cfg, keyType, Double.parseDouble(m.group(1).replace(",", "")));
                } catch (NumberFormatException ignored) {
                    // bad number — skip
                }
                break;
            }
        }
    }
}
