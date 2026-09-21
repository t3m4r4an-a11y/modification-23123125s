package net.macos.client.mixin;

import net.macos.client.utils.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public class CameraMixin {

    @Redirect(method = "update", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/entity/Entity;getYaw(F)F"
    ))
    private float macclient$getYaw(Entity instance, float tickDelta) {
        float base = instance.getYaw(tickDelta);
        if (FreeLook.active && instance == MinecraftClient.getInstance().player) {
            return base + FreeLook.getYawOffset(tickDelta);
        }
        return base;
    }

    @Redirect(method = "update", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/entity/Entity;getPitch(F)F"
    ))
    private float macclient$getPitch(Entity instance, float tickDelta) {
        float base = instance.getPitch(tickDelta);
        if (FreeLook.active && instance == MinecraftClient.getInstance().player) {
            return MathHelper.clamp(base + FreeLook.getPitchOffset(tickDelta), -90f, 90f);
        }
        return base;
    }
}