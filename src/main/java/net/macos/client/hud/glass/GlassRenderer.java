package net.macos.client.hud.glass;

import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * GPU SDF-Accelerated Liquid Glass renderer.
 * 100% Subpixel anti-aliasing, zero pixelation/stair-stepping.
 */
public final class GlassRenderer {

    private GlassRenderer() {}

    /** GPU Anti-aliased rounded rectangle fill */
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        SquircleRenderer.fill(ctx, x, y, w, h, r, color);
    }

    /** 1px rectangular border */
    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        SquircleRenderer.border(ctx, x, y, w, h, 0, 1.0f, color);
    }

    /** GPU Anti-aliased rounded border stroke */
    public static void roundedBorder(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        SquircleRenderer.border(ctx, x, y, w, h, r, 1.0f, color);
    }

    /** GPU Subpixel ambient drop shadow with smooth Gaussian-like hermite falloff */
    public static void dropShadow(DrawContext ctx, int x, int y, int w, int h, int r, int blurRadius, int maxAlpha) {
        SquircleRenderer.shadow(ctx, x, y, w, h, r, blurRadius, (maxAlpha << 24));
    }

    /** Top accent line */
    public static void topAccent(DrawContext ctx, int x, int y, int w, int color) {
        SquircleRenderer.fill(ctx, x, y, w, 1.5f, 0.75f, color);
    }

    /** Anti-aliased specular gloss strip under top edge */
    public static void specular(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int stripH = Math.max(1, Math.min(2, h / 16));
        float inset = Math.max(1f, radius / 2f);
        SquircleRenderer.fill(ctx, x + inset, y + 1, w - inset * 2, stripH, stripH / 2f, color);
    }

    /** GPU Anti-aliased soft neon bloom glow */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int radius, int color, int layers) {
        SquircleRenderer.shadow(ctx, x, y, w, h, radius, Math.max(4f, layers * 4f), color);
    }

    /** GPU Anti-aliased gradient overlay (top highlight / bottom depth) */
    public static void gradientOverlay(DrawContext ctx, int x, int y, int w, int h, int r, int topAlpha, int botAlpha) {
        if (topAlpha > 0) {
            SquircleRenderer.fillGradient(ctx, x, y, w, h / 2f, r, (topAlpha << 24) | 0xFFFFFF, 0x00FFFFFF);
        }
        if (botAlpha > 0) {
            SquircleRenderer.fillGradient(ctx, x, y + h / 2f, w, h / 2f, r, 0x00000000, (botAlpha << 24));
        }
    }

    /** Complete Liquid Glass Panel: Glow + Fill + Gradient + Specular + Border + Accent */
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
        roundedBorder(ctx, x, y, w, h, s.radius, s.borderColor);
        if (s.topAccent) {
            topAccent(ctx, x, y, w, s.accentColor);
        }
    }
}
