package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;

import net.macos.client.utils.Zoom;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;

import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // ============================================================
    // ZOOM
    // ============================================================
    @Inject(
        method = "getFov",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onChangeFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (Zoom.isZooming) {
            float fov = Zoom.getCurrentFov(tickDelta);
            if (fov > 0) {
                cir.setReturnValue((double) fov);
            }
        }
    }

    // ============================================================
    // REMOVE PUNCH
    // ============================================================
    @Inject(
        method = "tiltViewWhenHurt",
        at = @At("HEAD"),
        cancellable = true
    )
    private void macclient$removePunch(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (ConfigManager.INSTANCE.removePunch) {
            ci.cancel();
        }
    }
}