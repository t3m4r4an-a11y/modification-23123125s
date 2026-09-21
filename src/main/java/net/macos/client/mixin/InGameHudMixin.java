package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
        method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderEffects(CallbackInfo ci) {
        if (ConfigManager.INSTANCE.enablePotionHud
                && ConfigManager.INSTANCE.hideVanillaEffects) {
            ci.cancel();
        }
    }
}