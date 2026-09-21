package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.macos.client.gui.toast.ToastType;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class VanillaToastManagerMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void macclient$interceptAdvancement(Toast toast, CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableCustomToasts) return;
        if (!(toast instanceof AdvancementToast advToast)) return;

        Advancement advancement = ((AdvancementToastAccessor) advToast).getAdvancement();
        if (advancement == null) return;

        AdvancementDisplay display = advancement.getDisplay();
        if (display == null) return;

        String title = display.getTitle().getString();
        String desc = display.getDescription().getString();

        // Полное имя — без конфликта импортов
        net.macos.client.gui.toast.ToastManager.show(title, desc, ToastType.SUCCESS);
        ci.cancel();
    }
}