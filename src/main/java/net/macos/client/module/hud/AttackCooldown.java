package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class AttackCooldown {

    private static final float CRIT_THRESHOLD = 0.9f;
    private static final int ARC_RADIUS = 14;      // ФИКСИРОВАННЫЙ радиус дуги
    private static final int ARC_OFFSET_Y = 3;     // отступ вниз от центра

    private float smoothProgress = 1.0f;
    private long critReadyTime = 0;
    private boolean wasCritReady = true;

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!ConfigManager.INSTANCE.enableAttackCooldown) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float progress = mc.player.getAttackCooldownProgress(0f);
        smoothProgress += (progress - smoothProgress) * Math.min(1f, delta * 12f);

        boolean critReady = progress >= CRIT_THRESHOLD;
        if (critReady && !wasCritReady) {
            critReadyTime = System.currentTimeMillis();
        }
        wasCritReady = critReady;

        long sinceCrit = System.currentTimeMillis() - critReadyTime;

        // Точка центра — идентична crosshair
        int cx = ctx.getScaledWindowWidth() / 2 + ConfigManager.INSTANCE.attackCooldownOffsetX;
        int cy = ctx.getScaledWindowHeight() / 2 + ConfigManager.INSTANCE.attackCooldownOffsetY;

        int pivotY = cy + ARC_OFFSET_Y;

        // Fade out при полной зарядке
        if (progress >= 1.0f) {
            if (sinceCrit > 350) return;
            float fade = 1f - (sinceCrit / 350f);
            int a = (int) (fade * 200);
            drawU(ctx, cx, pivotY, ARC_RADIUS, 0f, 1f, (a << 24) | 0x44FF66);
            return;
        }

        int color;
        if (critReady) {
            color = 0xFF44FF66;
        } else {
            float t = smoothProgress / CRIT_THRESHOLD;
            int r = 255;
            int g = (int) (60 + t * 150);
            int b = (int) (60 - t * 40);
            color = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        drawU(ctx, cx, pivotY, ARC_RADIUS, 0f, smoothProgress, color);

        // Пульсирующая точка при крите
        if (critReady) {
            float pulse = 1f;
            if (sinceCrit < 300) {
                pulse = 1f + (1f - sinceCrit / 300f) * 0.5f;
            }
            int dotR = (int) (2 * pulse);
            ctx.fill(cx - dotR, pivotY - ARC_RADIUS - dotR - 2,
                     cx + dotR, pivotY - ARC_RADIUS + dotR - 2, 0xFF44FF66);
        }
    }

    /**
     * U-дуга под точкой (cx, pivotY). 0 = лево, 1 = право.
     * Идёт ПО ЧАСОВОЙ от правой стороны вниз к левой.
     */
    private void drawU(DrawContext ctx, int cx, int pivotY, int radius,
                        float startT, float endT, int color) {
        if (endT <= startT) return;

        // Идём от ПРАВОГО бока (angle=0) вниз (90) к ЛЕВОМУ (180)
        float startAngle = 0f + (180f * startT);
        float endAngle = 0f + (180f * endT);
        float step = 2f;

        for (float angle = startAngle; angle <= endAngle; angle += step) {
            float rad = (float) Math.toRadians(angle);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = pivotY + (int) (Math.sin(rad) * radius);
            ctx.fill(px - 1, py - 1, px, py, color);
        }
    }
}