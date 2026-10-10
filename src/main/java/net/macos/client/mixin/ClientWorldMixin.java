package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(method = "getSkyColor(Lnet/minecraft/util/math/Vec3d;F)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
    private void onChangeSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (!ConfigManager.INSTANCE.enableWorldModulation) return;
        String tint = ConfigManager.INSTANCE.worldTint;
        if ("Cyberpunk".equalsIgnoreCase(tint)) {
            cir.setReturnValue(Vec3d.unpackRgb(0x551177)); // Neon violet
        } else if ("Cold Ice".equalsIgnoreCase(tint)) {
            cir.setReturnValue(Vec3d.unpackRgb(0x103550)); // Cold arctic cyan
        } else if ("Deep Dark".equalsIgnoreCase(tint)) {
            cir.setReturnValue(Vec3d.unpackRgb(0x05050A)); // Deep void midnight
        } else if ("Warm Sunset".equalsIgnoreCase(tint)) {
            cir.setReturnValue(Vec3d.unpackRgb(0x7A2810)); // Warm fiery sunset
        }
    }
}
