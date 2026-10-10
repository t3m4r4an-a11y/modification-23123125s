package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {

    @Shadow public abstract boolean isClient();

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (!this.isClient()) return;
        if (!ConfigManager.INSTANCE.enableWorldModulation) return;
        String mode = ConfigManager.INSTANCE.worldTime;
        if ("Day".equalsIgnoreCase(mode)) {
            cir.setReturnValue(6000L);
        } else if ("Sunset".equalsIgnoreCase(mode)) {
            cir.setReturnValue(12500L);
        } else if ("Midnight".equalsIgnoreCase(mode)) {
            cir.setReturnValue(18000L);
        }
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (!this.isClient()) return;
        if (!ConfigManager.INSTANCE.enableWorldModulation) return;
        String mode = ConfigManager.INSTANCE.worldWeather;
        if ("Clear".equalsIgnoreCase(mode)) {
            cir.setReturnValue(0.0f);
        } else if ("Rain".equalsIgnoreCase(mode) || "Thunder".equalsIgnoreCase(mode)) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (!this.isClient()) return;
        if (!ConfigManager.INSTANCE.enableWorldModulation) return;
        String mode = ConfigManager.INSTANCE.worldWeather;
        if ("Clear".equalsIgnoreCase(mode)) {
            cir.setReturnValue(0.0f);
        } else if ("Thunder".equalsIgnoreCase(mode)) {
            cir.setReturnValue(1.0f);
        }
    }
}
