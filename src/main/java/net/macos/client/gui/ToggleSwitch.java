package net.macos.client.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Ultra-stable, high-performance iOS/macOS toggle switch.
 * Rendered using guaranteed native DrawContext geometry with smooth rounded pill and specular knob.
 */
public final class ToggleSwitch {

    private ToggleSwitch() {}

    public static final int WIDTH = 34;
    public static final int HEIGHT = 18;

    public static void draw(DrawContext ctx, int x, int y, float t, int accentRGB) {
        t = Math.max(0f, Math.min(1f, t));

        // Background Track: dark slate grey when OFF, vivid accent when ON
        int offTrack = 0x66333D4D;
        int onTrack  = 0xE6000000 | (accentRGB & 0xFFFFFF);
        int trackColor = blend(offTrack, onTrack, t);

        // Track body
        drawPillFast(ctx, x, y, WIDTH, HEIGHT, trackColor);

        // Track border
        int offBorder = 0x33FFFFFF;
        int onBorder  = 0x66000000 | (accentRGB & 0xFFFFFF);
        drawPillBorderFast(ctx, x, y, WIDTH, HEIGHT, blend(offBorder, onBorder, t));

        // Knob positioning
        int knobD = 12; // diameter
        int travel = WIDTH - knobD - 4; // 18px travel
        int knobX = x + 2 + (int) (travel * t);
        int knobY = y + 3;

        // Knob shadow
        fillCircleFast(ctx, knobX + knobD / 2, knobY + knobD / 2 + 1, knobD / 2, 0x4D000000);
        // Knob body (pure white)
        fillCircleFast(ctx, knobX + knobD / 2, knobY + knobD / 2, knobD / 2, 0xFFFFFFFF);
        // Knob top specular highlight
        fillCircleFast(ctx, knobX + knobD / 2, knobY + knobD / 2 - 1, knobD / 2 - 2, 0x40FFFFFF);
    }

    public static boolean isInside(int x, int y, int mx, int my) {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + HEIGHT;
    }

    public static int blend(int colorA, int colorB, float t) {
        int aA = (colorA >>> 24) & 0xFF, aR = (colorA >> 16) & 0xFF, aG = (colorA >> 8) & 0xFF, aB = colorA & 0xFF;
        int bA = (colorB >>> 24) & 0xFF, bR = (colorB >> 16) & 0xFF, bG = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
        int a = (int) (aA + (bA - aA) * t);
        int r = (int) (aR + (bR - aR) * t);
        int g = (int) (aG + (bG - aG) * t);
        int b = (int) (aB + (bB - aB) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void drawPillFast(DrawContext ctx, int x, int y, int w, int h, int color) {
        int r = h / 2;
        ctx.fill(x + r, y, x + w - r, y + h, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i - 1;
            int span = (int) Math.sqrt(r * r - dy * dy);
            ctx.fill(x + r - span, y + i, x + r, y + i + 1, color);
            ctx.fill(x + w - r, y + i, x + w - r + span, y + i + 1, color);
            ctx.fill(x + r - span, y + h - 1 - i, x + r, y + h - i, color);
            ctx.fill(x + w - r, y + h - 1 - i, x + w - r + span, y + h - i, color);
        }
    }

    private static void drawPillBorderFast(DrawContext ctx, int x, int y, int w, int h, int color) {
        int r = h / 2;
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i - 1;
            int span = (int) Math.sqrt(r * r - dy * dy);
            ctx.fill(x + r - span, y + i, x + r - span + 1, y + i + 1, color);
            ctx.fill(x + w - r + span - 1, y + i, x + w - r + span, y + i + 1, color);
            ctx.fill(x + r - span, y + h - 1 - i, x + r - span + 1, y + h - i, color);
            ctx.fill(x + w - r + span - 1, y + h - 1 - i, x + w - r + span, y + h - i, color);
        }
    }

    private static void fillCircleFast(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            int span = (int) Math.sqrt(r * r - i * i);
            ctx.fill(cx - span, cy + i, cx + span, cy + i + 1, color);
        }
    }
}