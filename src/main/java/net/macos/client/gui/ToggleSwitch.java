package net.macos.client.gui;

import net.minecraft.client.gui.DrawContext;

/** iOS-style пилюля-переключатель для булевых опций меню. */
public final class ToggleSwitch {

    private ToggleSwitch() {}

    public static final int WIDTH = 34;
    public static final int HEIGHT = 18;

    /** @param t 0 = выключено, 1 = включено — анимируй снаружи. */
    public static void draw(DrawContext ctx, int x, int y, float t, int accentRGB) {
        t = Math.max(0f, Math.min(1f, t));

        int offTrack = 0x40FFFFFF;
        int onTrack = 0xFF000000 | accentRGB;
        int track = blend(offTrack, onTrack, t);

        drawPill(ctx, x, y, WIDTH, HEIGHT, track);

        int travel = WIDTH - HEIGHT;
        int knobX = x + 2 + (int) (travel * t);
        int knobY = y + 2;
        int knobSize = HEIGHT - 4;
        drawPill(ctx, knobX, knobY, knobSize, knobSize, 0xFFFFFFFF);
    }

    public static boolean isInside(int x, int y, int mx, int my) {
        return mx >= x && mx <= x + WIDTH && my >= y && my <= y + HEIGHT;
    }

    private static int blend(int colorA, int colorB, float t) {
        int aA = (colorA >>> 24) & 0xFF, aR = (colorA >> 16) & 0xFF, aG = (colorA >> 8) & 0xFF, aB = colorA & 0xFF;
        int bA = (colorB >>> 24) & 0xFF, bR = (colorB >> 16) & 0xFF, bG = (colorB >> 8) & 0xFF, bB = colorB & 0xFF;
        int a = (int) (aA + (bA - aA) * t);
        int r = (int) (aR + (bR - aR) * t);
        int g = (int) (aG + (bG - aG) * t);
        int b = (int) (aB + (bB - aB) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void drawPill(DrawContext ctx, int x, int y, int w, int h, int color) {
        int r = h / 2;
        if (w <= h) { fillCircle(ctx, x + w / 2, y + h / 2, r, color); return; }
        ctx.fill(x + r, y, x + w - r, y + h, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int span = (int) Math.sqrt(Math.max(0, (long) r * r - (long) dy * dy));
            ctx.fill(x + r - span, y + i, x + r + span, y + i + 1, color);
            ctx.fill(x + w - r - span, y + i, x + w - r + span, y + i + 1, color);
        }
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int yOff = -r; yOff <= r; yOff++) {
            int span = (int) Math.sqrt(Math.max(0, r * r - yOff * yOff));
            ctx.fill(cx - span, cy + yOff, cx + span + 1, cy + yOff + 1, color);
        }
    }
}