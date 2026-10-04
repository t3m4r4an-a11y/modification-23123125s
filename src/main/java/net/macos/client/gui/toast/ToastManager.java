package net.macos.client.gui.toast;

import net.macos.client.gui.icon.IconRenderer;
import net.macos.client.hud.glass.GlassRenderer;
import net.macos.client.render.GlowRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ToastManager {

    // ============================================================
    // КОНСТАНТЫ
    // ============================================================

    private static final int MAX_VISIBLE = 4;
    private static final int WIDTH       = 240;
    private static final int HEIGHT      = 56;
    private static final int GAP         = 10;
    private static final int TOP_MARGIN  = 12;
    private static final int RADIUS      = 14;

    // ============================================================
    // СОСТОЯНИЕ
    // ============================================================

    private static final List<Toast> toasts = new ArrayList<>();

    // ============================================================
    // PUBLIC API
    // ============================================================

    public static void show(String title, String description, ToastType type) {
        toasts.add(new Toast(title, description, type));
        while (toasts.size() > MAX_VISIBLE) {
            toasts.remove(0);
        }
    }

    public static void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int screenW = ctx.getScaledWindowWidth();
        int centerX = screenW / 2;

        Iterator<Toast> it = toasts.iterator();
        int index = 0;
        while (it.hasNext()) {
            Toast t = it.next();
            if (t.isDone()) {
                it.remove();
                continue;
            }
            renderOne(ctx, mc, t, centerX, TOP_MARGIN + index * (HEIGHT + GAP));
            index++;
        }
    }

    // ============================================================
    // RENDER ONE TOAST
    // ============================================================

    private static void renderOne(DrawContext ctx, MinecraftClient mc, Toast t,
                                  int centerX, int targetY) {
        float p = t.getProgress();
        if (p <= 0.001f) return;

        // ── Morph: капля → плашка
        int w = (int) lerp(40f, WIDTH, p);
        int h = (int) lerp(40f, HEIGHT, p);
        int x = centerX - w / 2;
        int y = (int) lerp(-h - 16, targetY, p);
        int radius = (int) lerp(h / 2f, RADIUS, p);
        int alpha = (int) (p * 255);

        int accent = t.type.getColor();
        int accentRGB = accent & 0xFFFFFF;

        // ── 1. Glow (SDF, additive)
        float glowSize = 16f * p;
        int glowAlpha = (int) (alpha * 0.38f);
        if (glowAlpha > 2) {
            GlowRenderer.draw(x, y, w, h, radius, glowSize,
                    (glowAlpha << 24) | accentRGB);
        }

        // ── 2. Glass background
        int bgAlpha = (int) (alpha * 0.62f);
        int bgColor = (bgAlpha << 24) | 0x0E1420;
        GlassRenderer.roundedRect(ctx, x, y, w, h, radius, bgColor);

        // ── 3. Border (accent-tinted, 1px)
        int borderAlpha = (int) (alpha * 0.35f);
        int borderColor = (borderAlpha << 24) | accentRGB;
        drawBorder(ctx, x, y, w, h, radius, borderColor);

        // ── 4. Specular highlight (тонкая белая линия сверху)
        GlassRenderer.specular(ctx, x, y, w, h, radius,
                ((int) (alpha * 0.30f) << 24) | 0xFFFFFF);

        // ── 5. Icon (масштабируется при морфе)
        int iconSize = (int) (12 * p);
        if (iconSize >= 6) {
            int iconX = x + 18;
            int iconY = y + (h - iconSize) / 2 - 2;
            float iconScale = iconSize / 10f;
            int iconColor = (alpha << 24) | accentRGB;
            drawIconScaled(ctx, t.type.icon, iconX, iconY, iconScale, iconColor);
        }

        // ── 6. Text
        int textX = x + 46;
        int titleColor = (alpha << 24) | 0xF5F5F7;
        int descColor  = (alpha << 24) | 0xA1A1A6;

        ctx.drawTextWithShadow(mc.textRenderer, t.title,       textX, y + 14, titleColor);
        ctx.drawTextWithShadow(mc.textRenderer, t.description, textX, y + 32, descColor);
    }

    // ============================================================
    // ХЕЛПЕРЫ
    // ============================================================

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * 1px border по контуру rounded rect. Углы — только внешнее кольцо.
     */
    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h,
                                   int radius, int color) {
        // Прямые сегменты
        ctx.fill(x + radius, y, x + w - radius, y + 1, color);              // top
        ctx.fill(x + radius, y + h - 1, x + w - radius, y + h, color);      // bottom
        ctx.fill(x, y + radius, x + 1, y + h - radius, color);              // left
        ctx.fill(x + w - 1, y + radius, x + w, y + h - radius, color);      // right

        // Углы — тонкое кольцо
        for (int i = 0; i < radius; i++) {
            float dx = radius - i - 0.5f;
            for (int j = 0; j < radius; j++) {
                float dy = radius - j - 0.5f;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d >= radius - 1.5f && d <= radius + 0.5f) {
                    ctx.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                    ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, color);
                    ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, color);
                    ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, color);
                }
            }
        }
    }

    private static void drawIconScaled(DrawContext ctx, String icon,
                                       int x, int y, float scale, int color) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        IconRenderer.draw(ctx, icon, 0, 0, color);
        ctx.getMatrices().pop();
    }
}