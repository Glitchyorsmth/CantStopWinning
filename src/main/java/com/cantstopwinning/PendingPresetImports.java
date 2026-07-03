package com.cantstopwinning;

import dev.anvil.api.Framework;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presets staged by a composite {@link ImportConfirmMode#CONFIRM} import that collided with an
 * existing local preset of the same name, waiting on the user to click the confirm link posted to
 * chat. Auto-expires (dropped, original left untouched) 5 seconds after staging if never
 * confirmed — either way, a chat line reports what happened. Pure JVM state, not persisted —
 * deliberately lost on disconnect/restart.
 */
final class PendingPresetImports {

    // Plain map — everything here runs on the client thread (UI clicks, Fabric command dispatch,
    // Anvil's tick-driven scheduler), never concurrently.
    private static final Map<String, List<String>> pending = new HashMap<>();

    private PendingPresetImports() {
    }

    /** Stages {@code triggers} under {@code name}; auto-drops in 5s if never confirmed. */
    static void stage(String name, List<String> triggers) {
        pending.put(name, triggers);
        Framework.scheduler().delayMs(5000, () -> {
            if (pending.remove(name) != null) {
                message("§7" + name + "§r wasn't confirmed in time — kept the old one.");
            }
        });
    }

    /**
     * Applies the staged import for {@code name}, if it's still pending.
     *
     * @return {@code true} if it was applied; {@code false} if it already expired or was never staged
     */
    static boolean confirm(String name) {
        List<String> triggers = pending.remove(name);
        if (triggers == null) {
            message("§7" + name + "§r already expired — nothing to confirm.");
            return false;
        }
        CswConfig cfg = CantStopWinningClient.CONFIG.get();
        cfg.presets.put(name, triggers);
        CantStopWinningClient.CONFIG.save();
        message("§a" + name + "§r overwritten.");
        return true;
    }

    private static void message(String text) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Component msg = Component.literal("§b[CSW]§r " + text);
        // Renamed across the boundary: displayClientMessage(Component, boolean) split into
        // sendSystemMessage(Component) / sendOverlayMessage(Component) on 26.x.
        //? if <26.2 {
        player.displayClientMessage(msg, false);
        //?}
        //? if >=26.2 {
        /*player.sendSystemMessage(msg);*/
        //?}
    }
}
