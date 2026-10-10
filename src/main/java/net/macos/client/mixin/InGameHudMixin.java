package net.macos.client.mixin;

import net.macos.client.config.ConfigManager;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Shadow @Final
    private MinecraftClient client;

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

    @Inject(
        method = "renderHotbar(FLnet/minecraft/client/gui/DrawContext;)V",
        at = @At("TAIL")
    )
    private void macclient$renderSelectedHotbarHighlight(float tickDelta, DrawContext context, CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableHotbarHighlight) return;
        if (client == null || client.player == null) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int hotbarX = screenWidth / 2 - 91;
        int hotbarY = screenHeight - 22;

        int selected = client.player.getInventory().selectedSlot;
        if (selected < 0 || selected > 8) return;

        int slotX = hotbarX - 1 + selected * 20;
        int slotY = hotbarY - 1;

        int accent = 0x00D4FF;
        try {
            accent = Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
        } catch (Exception ignored) {}

        // Ambient soft neon glow
        GlassRenderer.glow(context, slotX + 1, slotY + 1, 22, 22, 4, (0x50 << 24) | accent, 2);
        // Translucent liquid glass highlight fill
        SquircleRenderer.fill(context, slotX + 1, slotY + 1, 22, 22, 4, (0x30 << 24) | accent);
        // Anti-aliased accent border
        SquircleRenderer.border(context, slotX + 1, slotY + 1, 22, 22, 4, 1.2f, (0xEE << 24) | accent);
    }
}