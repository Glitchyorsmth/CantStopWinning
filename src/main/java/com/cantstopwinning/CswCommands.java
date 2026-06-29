package com.cantstopwinning;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.anvil.api.Framework;
import dev.anvil.api.util.Texts;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * The {@code /csw} command tree, registered through Anvil. Built directly with Brigadier (Anvil's
 * recommended approach — version-stable across 1.21.11 / 26.1) and handed to
 * {@code Framework.commands().registerRaw}, which re-registers it on every world join.
 */
public final class CswCommands {

    private CswCommands() {
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> lit(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static void register() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = lit("csw")
                .executes(ctx -> {
                    openConfig(); // bare /csw opens the settings screen
                    return 1;
                })
                .then(lit("config").executes(ctx -> {
                    openConfig();
                    return 1;
                }))
                .then(lit("on").executes(ctx -> {
                    setEnabled(true);
                    ctx.getSource().sendFeedback(Texts.literal("CantStopWinning enabled."));
                    return 1;
                }))
                .then(lit("off").executes(ctx -> {
                    setEnabled(false);
                    ctx.getSource().sendFeedback(Texts.literal("CantStopWinning disabled."));
                    return 1;
                }))
                .then(lit("test").executes(ctx -> {
                    Celebrations.win();
                    ctx.getSource().sendFeedback(Texts.literal("Celebration triggered!"));
                    return 1;
                }))
                .then(lit("testloss").executes(ctx -> {
                    Celebrations.loss();
                    ctx.getSource().sendFeedback(Texts.literal("Loss overlay triggered!"));
                    return 1;
                }))
                .then(lit("sim").then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                                "message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String msg = StringArgumentType.getString(ctx, "message");
                            ctx.getSource().sendFeedback(Texts.literal("Simulating: " + msg));
                            CswChat.onChat(Texts.literal(msg)); // run the real detector on fake text
                            return 1;
                        })))
                .then(lit("help").executes(ctx -> {
                    var src = ctx.getSource();
                    src.sendFeedback(Texts.literal("/csw          - open settings"));
                    src.sendFeedback(Texts.literal("/csw on|off   - toggle mod"));
                    src.sendFeedback(Texts.literal("/csw test     - fire celebration"));
                    src.sendFeedback(Texts.literal("/csw testloss - fire loss overlay"));
                    src.sendFeedback(Texts.literal("/csw sim <msg>- test chat detection"));
                    src.sendFeedback(Texts.literal("/csw config   - open settings"));
                    src.sendFeedback(Texts.literal("/csw help     - this help"));
                    return 1;
                }));

        Framework.commands().registerRaw(root);
    }

    private static void setEnabled(boolean on) {
        CantStopWinningClient.CONFIG.get().enabled = on;
        CantStopWinningClient.CONFIG.save();
    }

    private static void openConfig() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(buildScreen()));
    }

    /**
     * The custom screen uses {@code GuiGraphics} (1.21.11 only); 26.x falls back to Anvil's
     * generated screen. The build excludes {@code CswConfigScreen} on 26.x.
     */
    private static Screen buildScreen() {
        //? if <26.1 {
        return new CswConfigScreen(CantStopWinningClient.CONFIG);
        //?}
        //? if >=26.1 {
        /*return Framework.gui().configScreen(CantStopWinningClient.CONFIG);*/
        //?}
    }
}
