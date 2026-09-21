package net.macos.client.gui.toast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ToastManager {
    private static final List<Toast> toasts = new ArrayList<>();
    private static final int MAX_VISIBLE = 4;
    private static final int WIDTH = 200;
    private static final int HEIGHT = 44;
    private static final int GAP = 6;
    private static final int TOP_MARGIN = 8;

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

    private static void renderOne(DrawContext ctx, MinecraftClient mc, Toast t, int centerX, int targetY) {
        float p = t.getProgress();
        if (p <= 0.001f) return;

        // Морфинг капля → плашка
        int w = (int) lerp(30f, WIDTH, p);
        int h = (int) lerp(30f, HEIGHT, p);
        int x = centerX - w / 2;
        int y = (int) lerp(-h - 8, targetY, p);
        int radius = (int) lerp(h / 2f, 10f, p);
        int alpha = (int) (p * 255);

        int accent = t.type.getColor();
        int accentRGB = accent & 0xFFFFFF;

        // === Свечение — мягкий градиент (6 тонких слоёв) ===
        for (int i = 6; i >= 1; i--) {
            float fade = (6 - i) / 6f;              // 0 → 0.83
            int glowAlpha = (int) (alpha * 0.14f * fade);
            if (glowAlpha <= 1) continue;
            int glow = (glowAlpha << 24) | accentRGB;
            fillRounded(ctx, x - i, y - i, w + i * 2, h + i * 2, radius + i, glow);
        }

        // === Внешняя рамка (accent) ===
        int border = (alpha << 24) | accentRGB;
        fillRounded(ctx, x, y, w, h, radius, border);

        // === Внутренний фон ===
        int bg = (alpha << 24) | 0x121218;
        fillRounded(ctx, x + 2, y + 2, w - 4, h - 4, Math.max(1, radius - 1), bg);

        // === Иконка (кружок accent) ===
        int iconCX = x + 16;
        int iconCY = y + h / 2;
        drawCircle(ctx, iconCX, iconCY, 8, border);
        drawCircle(ctx, iconCX, iconCY, 6, (alpha << 24) | 0x121218);

        int symColor = (alpha << 24) | 0xFFFFFF;
        int symW = mc.textRenderer.getWidth(t.type.symbol);
        ctx.drawTextWithShadow(mc.textRenderer, t.type.symbol,
                iconCX - symW / 2, iconCY - 4, symColor);

        // === Текст ===
        int textX = x + 32;
        int titleColor = (alpha << 24) | 0xFFFFFF;
        int descColor = (alpha << 24) | 0xA8A8B0;
        ctx.drawTextWithShadow(mc.textRenderer, t.title, textX, y + 9, titleColor);
        ctx.drawTextWithShadow(mc.textRenderer, t.description, textX, y + 25, descColor);
    }

    // ============================================================
    // ХЕЛПЕРЫ
    // ============================================================
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void fillRounded(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));

        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                float dx = r - i - 0.5f;
                float dy = r - j - 0.5f;
                if (dx * dx + dy * dy <= r * r) {
                    ctx.fill(x + i, y + j, x + i + 1, y + j + 1, color);
                    ctx.fill(x + w - 1 - i, y + j, x + w - i, y + j + 1, color);
                    ctx.fill(x + i, y + h - 1 - j, x + i + 1, y + h - j, color);
                    ctx.fill(x + w - 1 - i, y + h - 1 - j, x + w - i, y + h - j, color);
                }
            }
        }
    }

    private static void drawCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y <= r * r) {
                    ctx.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
    }
}