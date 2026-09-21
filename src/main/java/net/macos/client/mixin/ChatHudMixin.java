package net.macos.client.mixin;

import net.macos.client.chat.ChatRenderer;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow @Final
    private List<ChatHudLine.Visible> visibleMessages;

    @Shadow
    private int scrolledLines;

    @Shadow
    public abstract int getLineHeight();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void macclient$render(DrawContext ctx, int currentTick, int mouseX, int mouseY,
                                    CallbackInfo ci) {
        if (!ConfigManager.INSTANCE.enableCustomChat) return;

        ChatRenderer.renderFromHud(ctx, visibleMessages, scrolledLines, getLineHeight(), currentTick);
        ci.cancel();
    }
}