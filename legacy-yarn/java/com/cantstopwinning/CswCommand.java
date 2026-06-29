package com.cantstopwinning;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CswCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("csw")
            .then(literal("on").executes(ctx -> {
                CantStopWinningClient.CONFIG.enabled = true;
                CantStopWinningClient.CONFIG.save();
                send(ctx, "CantStopWinning enabled.");
                return 1;
            }))
            .then(literal("off").executes(ctx -> {
                CantStopWinningClient.CONFIG.enabled = false;
                CantStopWinningClient.CONFIG.save();
                send(ctx, "CantStopWinning disabled.");
                return 1;
            }))
            .then(literal("test").executes(ctx -> {
                CantStopWinningClient.triggerCelebration();
                send(ctx, "Celebration triggered!");
                return 1;
            }))
            .then(literal("testloss").executes(ctx -> {
                CantStopWinningClient.triggerLoss();
                send(ctx, "Loss overlay triggered!");
                return 1;
            }))
            .then(literal("config").executes(ctx -> {
                openConfig();
                return 1;
            }))
            .then(literal("help").executes(ctx -> {
                send(ctx, "/csw          - open config GUI");
                send(ctx, "/csw on|off   - toggle mod");
                send(ctx, "/csw test     - fire celebration");
                send(ctx, "/csw testloss - fire loss overlay");
                send(ctx, "/csw config   - open config GUI");
                send(ctx, "/csw help     - this help");
                return 1;
            }))
        );
    }

    public static void openConfig() {
        MinecraftClient.getInstance().execute(() ->
            MinecraftClient.getInstance().setScreen(
                new com.cantstopwinning.gui.ConfigScreen(null)));
    }

    private static void send(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Text.literal(msg));
    }
}
