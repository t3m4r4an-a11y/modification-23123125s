package net.macos.client.mixin;

import net.macos.client.render.ShulkerPreviewRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    protected Slot focusedSlot;

    @Inject(
        method = "drawMouseoverTooltip",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        if (focusedSlot != null && focusedSlot.hasStack()) {
            if (ShulkerPreviewRenderer.isShulkerBox(focusedSlot.getStack())) {
                ShulkerPreviewRenderer.renderPreview(context, focusedSlot.getStack(), x, y);
                ci.cancel(); // Don't draw the ugly vanilla list wall of text over/under it!
            }
        }
    }
}
