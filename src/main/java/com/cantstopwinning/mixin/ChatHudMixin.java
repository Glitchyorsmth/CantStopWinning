package com.cantstopwinning.mixin;

import com.cantstopwinning.CantStopWinningClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    // Matches any player chat message on Hypixel. Covers all formats:
    //   [451] [MVP++] Glitchyorsmth: message     (SkyBlock with level)
    //   [MVP+] PlayerName: message                (standard lobby)
    //   <PlayerName> message                       (vanilla format)
    //   Guild > [VIP] PlayerName: message          (guild/party/co-op)
    //   Party > PlayerName: message
    //   From [VIP] PlayerName: message             (DMs)
    //   To [VIP] PlayerName: message
    //
    // Key insight: real server system messages (coin digs, level ups, etc.)
    // NEVER contain "PlayerName:" with a colon. If we see a word followed by
    // a colon-space, it's a player talking.
    private static final Pattern PLAYER_CHAT_PATTERN = Pattern.compile(
            "(?:^|\\s)\\w{1,16}: "   // any word (1-16 chars) followed by ": " = player name
    );

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (CantStopWinningClient.CONFIG == null || !CantStopWinningClient.CONFIG.enabled) return;

        String plain = message.getString();

        // Strip color codes (§x) that Hypixel uses
        String stripped = plain.replaceAll("§.", "");

        // Skip if this looks like a player chat message
        if (PLAYER_CHAT_PATTERN.matcher(stripped).find()) return;

        List<String> triggers = List.copyOf(CantStopWinningClient.CONFIG.triggerMessages);
        for (String trigger : triggers) {
            if (stripped.contains(trigger)) {
                CantStopWinningClient.triggerCelebration();
                break;
            }
        }
    }
}
