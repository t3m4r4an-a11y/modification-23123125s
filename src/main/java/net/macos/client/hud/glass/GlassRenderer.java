package net.macos.client.hud.glass;

import net.minecraft.client.gui.DrawContext;

/**
 * Draws Liquid Glass panels: rounded fill, soft specular, border, optional glow.
 */
public final class GlassRenderer {

    private GlassRenderer() {}

    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));

        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        int baseA = (color >>> 24) & 0xFF;
        int rgb = color & 0xFFFFFF;

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                float dx = r - i - 0.5f;
                float dy = r - j - 0.5f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float cov = r - dist + 0.5f;
                if (cov <= 0f) continue;
                if (cov > 1f) cov = 1f;

                int a = (int) (baseA * cov);
                if (a <= 0) continue;
                int c = (a << 24) | rgb;

                ctx.fill(x + i, y + j, x + i + 1, y + j + 1, c);
                ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, c);
                ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, c);
                ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, c);
            }
        }
    }

    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void topAccent(DrawContext ctx, int x, int y, int w, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
    }

    /** Thin specular strip under the top edge — Liquid Glass highlight. */
    public static void specular(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        int stripH = Math.max(1, Math.min(3, h / 8));
        int inset = Math.max(1, radius / 2);
        int a = (color >>> 24) & 0xFF;
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < stripH; i++) {
            float f = 1f - (i / (float) stripH);
            int layerA = (int) (a * f);
            if (layerA <= 0) continue;
            int c = (layerA << 24) | rgb;
            int yPos = y + 1 + i;
            if (yPos >= y + h - 1) break;
            ctx.fill(x + inset, yPos, x + w - inset, yPos + 1, c);
        }
    }

    public static void glow(DrawContext ctx, int x, int y, int w, int h, int radius, int color, int layers) {
        for (int i = layers; i >= 1; i--) {
            float fade = (layers - i + 1) / (float) layers;
            int baseA = (color >>> 24) & 0xFF;
            int layerA = (int) (baseA * 0.15f * fade);
            if (layerA <= 1) continue;
            int c = (layerA << 24) | (color & 0xFFFFFF);
            roundedRect(ctx, x - i, y - i, w + i * 2, h + i * 2, radius + i, c);
        }
    }

    /** Soft vertical volume: light top, darker bottom. */
    public static void gradientOverlay(DrawContext ctx, int x, int y, int w, int h, int r, int topAlpha, int botAlpha) {
        if (topAlpha > 0) {
            int half = Math.max(1, h / 2);
            for (int i = 0; i < half; i++) {
                float f = 1f - (i / (float) half);
                int a = (int) (topAlpha * f);
                if (a <= 0) continue;
                int c = (a << 24) | 0xFFFFFF;
                int inset = 0;
                if (i < r && r > 0) {
                    double t = (r - i) / (double) r;
                    inset = (int) (r * (1.0 - Math.sqrt(Math.max(0.0, 1.0 - t * t))));
                }
                ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
            }
        }
        if (botAlpha > 0) {
            int half = Math.max(1, h / 2);
            for (int i = 0; i < half; i++) {
                float f = 1f - (i / (float) half);
                int a = (int) (botAlpha * f);
                if (a <= 0) continue;
                int c = (a << 24) | 0x000000;
                ctx.fill(x, y + h - 1 - i, x + w, y + h - i, c);
            }
        }
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, PanelStyle s) {
        if (s.glowLayers > 0) {
            glow(ctx, x, y, w, h, s.radius, s.glowColor, s.glowLayers);
        }
        roundedRect(ctx, x, y, w, h, s.radius, s.bgColor);
        if (s.gradientTop > 0 || s.gradientBot > 0) {
            int topA = (s.gradientTop >>> 24) & 0xFF;
            int botA = (s.gradientBot >>> 24) & 0xFF;
            gradientOverlay(ctx, x, y, w, h, s.radius, topA, botA);
        }
        if (s.specular) {
            specular(ctx, x, y, w, h, s.radius, s.specularColor);
        }
        border(ctx, x, y, w, h, s.borderColor);
        if (s.topAccent) {
            topAccent(ctx, x, y, w, s.accentColor);
        }
    }
}
