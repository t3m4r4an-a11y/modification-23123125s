package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class CustomCrosshair {

    public static void render(DrawContext ctx, float delta) {
        if (!ConfigManager.INSTANCE.enableCustomCrosshair) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int cx = (ctx.getScaledWindowWidth() - 1) / 2 + ConfigManager.INSTANCE.crosshairOffsetX;
        int cy = (ctx.getScaledWindowHeight() - 1) / 2 + ConfigManager.INSTANCE.crosshairOffsetY;

        int color = parseColor(ConfigManager.INSTANCE.crosshairColor);
        int outline = 0xA0000000;   // полупрозрачный чёрный

        int style = ConfigManager.INSTANCE.crosshairStyle;
        int size = ConfigManager.INSTANCE.crosshairSize;
        int gap = ConfigManager.INSTANCE.crosshairGap;
        int th = Math.max(1, ConfigManager.INSTANCE.crosshairThickness);

        // Динамический gap от замаха
        if (ConfigManager.INSTANCE.crosshairDynamic) {
            float cooldown = mc.player.getAttackCooldownProgress(0f);
            float expand = (1f - cooldown) * 4f;
            gap = (int) (gap + expand);
        }

        switch (style) {
            case 0 -> drawCross(ctx, cx, cy, size, gap, th, color, outline);
            case 1 -> drawDot(ctx, cx, cy, th, color, outline);
            case 2 -> drawCircle(ctx, cx, cy, size, th, color, outline);
            case 3 -> {
                drawCross(ctx, cx, cy, size, gap, th, color, outline);
                drawDot(ctx, cx, cy, 2, color, outline);
            }
        }
    }

    // ============================================================
    // СТИЛИ
    // ============================================================

    /** Крестик с обводкой: чёрный контур под белыми линиями */
    private static void drawCross(DrawContext ctx, int cx, int cy, int size, int gap, int th,
                                   int color, int outline) {
        // Обводка (на 1px больше во все стороны)
        drawCrossLines(ctx, cx, cy, size + 1, gap - 1, th + 2, outline);
        // Основной цвет
        drawCrossLines(ctx, cx, cy, size, gap, th, color);
    }

    private static void drawCrossLines(DrawContext ctx, int cx, int cy, int size, int gap, int th, int color) {
        // Верхняя линия
        ctx.fill(cx, cy - gap - size, cx + th, cy - gap, color);
        // Нижняя линия
        ctx.fill(cx, cy + gap, cx + th, cy + gap + size, color);
        // Левая линия
        ctx.fill(cx - gap - size, cy, cx - gap, cy + th, color);
        // Правая линия
        ctx.fill(cx + gap, cy, cx + gap + size, cy + th, color);
    }

    /** Точка в центре */
    private static void drawDot(DrawContext ctx, int cx, int cy, int size, int color, int outline) {
        int half = size / 2;
        // Обводка
        ctx.fill(cx - half - 1, cy - half - 1, cx - half + size + 1, cy - half + size + 1, outline);
        // Точка
        ctx.fill(cx - half, cy - half, cx - half + size, cy - half + size, color);
    }

    /** Тонкое кольцо вокруг центра */
    private static void drawCircle(DrawContext ctx, int cx, int cy, int radius, int th,
                                    int color, int outline) {
        float step = 4f;
        int half = th / 2;

        // Обводка
        for (float ang = 0; ang < 360; ang += step) {
            float rad = (float) Math.toRadians(ang);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = cy + (int) (Math.sin(rad) * radius);
            ctx.fill(px - half - 1, py - half - 1,
                     px - half + th + 1, py - half + th + 1, outline);
        }
        // Основное кольцо
        for (float ang = 0; ang < 360; ang += step) {
            float rad = (float) Math.toRadians(ang);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = cy + (int) (Math.sin(rad) * radius);
            ctx.fill(px - half, py - half, px - half + th, py - half + th, color);
        }
    }

    // ============================================================
    // ХЕЛПЕРЫ
    // ============================================================

    private static int parseColor(String hex) {
        try {
            return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}