package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void onPreRenderFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ConfigManager.INSTANCE.enableLowFire) {
            matrices.push();
            matrices.translate(0.0, -ConfigManager.INSTANCE.lowFireOffset, 0.0);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void onPostRenderFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ConfigManager.INSTANCE.enableLowFire) {
            matrices.pop();
        }
    }
}
