package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.macos.client.module.hud.CustomScoreboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ScoreboardMixin {

    @Inject(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void macclient$renderCustomScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableCustomScoreboard) return;

        CustomScoreboard.render(context, objective);
        ci.cancel();
    }
}