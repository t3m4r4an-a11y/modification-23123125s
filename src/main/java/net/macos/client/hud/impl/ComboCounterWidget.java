package net.macos.client.hud.impl;

import net.macos.client.MacClient;
import net.macos.client.hud.glass.GlassWidget;
import net.macos.client.hud.glass.PanelStyle;
import net.macos.client.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class ComboCounterWidget extends GlassWidget {

    private int combo = 0;
    private long lastHitTime = 0;
    private float hitScale = 1.0f;

    public ComboCounterWidget() {
        super("comboCounter");
    }

    public void onHit() {
        long now = System.currentTimeMillis();
        if (now - lastHitTime > 2000) {
            combo = 0;   // сбрасываем таймер комбо
        }
        combo++;
        lastHitTime = now;
        hitScale = 1.3f;
    }

    @Override
    protected boolean shouldBeVisible() {
        if (MacClient.hudEditorOpen) return true;
        if (combo == 0) return false;
        return System.currentTimeMillis() - lastHitTime <= 2000;
    }

    @Override
    protected void configureStyle(PanelStyle s) {
        s.radius = 6;
        s.bgColor = 0xC014141F;
        s.borderColor = 0x20FFFFFF;
        s.topAccent = false;
        s.gradientTop = 0;
        s.gradientBot = 0;
    }

    @Override
    protected void measure(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String text = combo + "x Combo";
        this.w = mc.textRenderer.getWidth(text) + 24;
        this.h = 24;
    }

    @Override
    protected void renderInner(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Медленный лерп scale обратно к 1.0
        hitScale += (1.0f - hitScale) * Math.min(1f, delta * 8f);

        MinecraftClient mc = MinecraftClient.getInstance();
        Color accent = RenderUtils.hexToColor(
            net.macos.client.config.ConfigManager.INSTANCE.accentColor);
        int textColor = applyAlpha((0xFF << 24) | (accent.getRGB() & 0xFFFFFF));

        String text = combo + "x Combo";
        int tw = mc.textRenderer.getWidth(text);

        // Scale от центра панели
        ctx.getMatrices().push();
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(hitScale, hitScale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        ctx.drawTextWithShadow(mc.textRenderer, text,
            x + (w - tw) / 2, y + (h - 8) / 2, textColor);

        ctx.getMatrices().pop();
    }
}