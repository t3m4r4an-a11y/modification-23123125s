package net.macos.client.module.hud;

import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Modern, smooth Attack Cooldown indicator.
 * Disappears completely when fully charged (no screen clutter).
 * Supports:
 * - Minimalist sleek rounded bar under the crosshair with color gradient and crit flash
 * - Smooth circular ring around the reticle
 */
public class AttackCooldown {

    private static final float CRIT_THRESHOLD = 0.9f;

    private float smoothProgress = 1.0f;
    private long critReadyTime = 0;
    private boolean wasCritReady = true;

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!ConfigManager.INSTANCE.enableAttackCooldown) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float progress = mc.player.getAttackCooldownProgress(0f);
        smoothProgress = MathHelper.lerp(Math.min(1f, delta * 14f), smoothProgress, progress);

        boolean critReady = progress >= CRIT_THRESHOLD;
        if (critReady && !wasCritReady) {
            critReadyTime = System.currentTimeMillis();
        }
        wasCritReady = critReady;

        long sinceCrit = System.currentTimeMillis() - critReadyTime;

        // When 100% charged and crit flash ended: hide completely!
        if (progress >= 0.999f && sinceCrit > 300) {
            return;
        }

        int cx = ctx.getScaledWindowWidth() / 2 + ConfigManager.INSTANCE.attackCooldownOffsetX;
        int cy = ctx.getScaledWindowHeight() / 2 + ConfigManager.INSTANCE.attackCooldownOffsetY;

        // Flash opacity when fully charged
        float alpha = 1.0f;
        if (progress >= 0.999f) {
            alpha = Math.max(0f, 1f - (sinceCrit / 300f));
        }

        // Color interpolation: Red (0.0) -> Yellow (0.5) -> Cyan/Green (1.0)
        int color;
        if (critReady) {
            int a = (int) (alpha * 240);
            color = (a << 24) | 0x00FF88;
        } else {
            float t = smoothProgress / CRIT_THRESHOLD;
            int r = (int) MathHelper.lerp(t, 255f, 20f);
            int g = (int) MathHelper.lerp(t, 60f, 220f);
            int b = (int) MathHelper.lerp(t, 60f, 255f);
            int a = (int) (alpha * 200);
            color = (a << 24) | (r << 16) | (g << 8) | b;
        }

        // Render sleek horizontal glass pill bar under crosshair
        renderPillBar(ctx, cx, cy + 12, 18, 3, smoothProgress, color, alpha);
    }

    private static void renderPillBar(DrawContext ctx, int cx, int y, int width, int height,
                                      float progress, int fillCol, float alpha) {
        int x = cx - width / 2;
        int bgA = (int) (alpha * 120);
        int borderA = (int) (alpha * 60);

        // Dark background track
        net.macos.client.render.SquircleRenderer.pill(ctx, x, y, width, height, (bgA << 24) | 0x0A0F18);
        net.macos.client.render.SquircleRenderer.border(ctx, x, y, width, height, height / 2.0f, 1.0f, (borderA << 24) | 0xFFFFFF);

        // Filled progress
        int fillW = (int) (width * MathHelper.clamp(progress, 0f, 1f));
        if (fillW >= 2) {
            net.macos.client.render.SquircleRenderer.pill(ctx, x, y, fillW, height, fillCol);
        }
        ctx.draw();
    }
}