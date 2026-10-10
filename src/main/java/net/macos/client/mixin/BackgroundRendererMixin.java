package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType,
                                    float viewDistance, boolean thickFog, float tickDelta,
                                    CallbackInfo ci) {
        if (ConfigManager.INSTANCE.enableNoFog) {
            ci.cancel();
        }
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void onApplyFogTail(Camera camera, BackgroundRenderer.FogType fogType,
                                       float viewDistance, boolean thickFog, float tickDelta,
                                       CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableWorldModulation) return;
        String tint = ConfigManager.INSTANCE.worldTint;
        if ("Cyberpunk".equalsIgnoreCase(tint)) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(0.42f, 0.12f, 0.65f, 1.0f);
        } else if ("Cold Ice".equalsIgnoreCase(tint)) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(0.12f, 0.45f, 0.68f, 1.0f);
        } else if ("Deep Dark".equalsIgnoreCase(tint)) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(0.02f, 0.02f, 0.04f, 1.0f);
        } else if ("Warm Sunset".equalsIgnoreCase(tint)) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(0.75f, 0.32f, 0.15f, 1.0f);
        }
    }
}