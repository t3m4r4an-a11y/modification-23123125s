package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class AspectRatioMixin {

    @Redirect(
        method = "getBasicProjectionMatrix",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/Window;getFramebufferWidth()I"
        )
    )
    private int macclient$modifyAspectWidth(Window window) {
        float multiplier = ConfigManager.INSTANCE.aspectRatio;
        if (multiplier <= 0f || multiplier == 1f) {
            return window.getFramebufferWidth();
        }
        // Растягиваем/сжимаем горизонтально → меняется соотношение
        return (int)(window.getFramebufferWidth() * multiplier);
    }
}