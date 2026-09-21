package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.macos.client.module.hud.CustomTabList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void macclient$renderCustomTab(DrawContext context, int scaledWindowWidth,
                                            Scoreboard scoreboard, ScoreboardObjective objective,
                                            CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableCustomTabList) return;

        CustomTabList.render(context, scaledWindowWidth, scoreboard, objective);
        ci.cancel();
    }
}