package net.macos.client.module.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.macos.client.render.SquircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/**
 * 2026 Precision Subpixel Tactical Reticle inspired by CS2, Valorant & Apex Legends.
 * Features:
 * - Perfectly centered subpixel geometry
 * - Anti-aliased SDF circle and ring reticles via GPU
 * - Dynamic cooldown expansion and target red-shift
 * - 5 Distinct Styles: Tactical Cross, Anti-aliased Precision Dot, Smooth Ring, Cross + Dot, Chevron Reticle
 */
public class CustomCrosshair {

    private static float currentGap = 3f;

    public static void render(DrawContext ctx, float delta) {
        if (!ConfigManager.INSTANCE.enableCustomCrosshair) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int cx = ctx.getScaledWindowWidth() / 2 + ConfigManager.INSTANCE.crosshairOffsetX;
        int cy = ctx.getScaledWindowHeight() / 2 + ConfigManager.INSTANCE.crosshairOffsetY;

        int baseColor = parseColor(ConfigManager.INSTANCE.crosshairColor);
        int outlineColor = 0xA0000000;

        // Turn red if aiming at a living entity
        if (isAimingAtEntity(mc)) {
            baseColor = 0xFFFF3344;
        }

        int style = ConfigManager.INSTANCE.crosshairStyle;
        int size = Math.max(1, ConfigManager.INSTANCE.crosshairSize);
        int baseGap = Math.max(0, ConfigManager.INSTANCE.crosshairGap);
        int th = Math.max(1, ConfigManager.INSTANCE.crosshairThickness);

        // Smooth dynamic gap expansion from attack cooldown
        float targetGap = baseGap;
        if (ConfigManager.INSTANCE.crosshairDynamic) {
            float cooldown = mc.player.getAttackCooldownProgress(0f);
            targetGap += (1.0f - cooldown) * 5.0f;
        }
        currentGap = MathHelper.lerp(Math.min(1.0f, delta * 16.0f), currentGap, targetGap);
        int gap = Math.round(currentGap);

        switch (style) {
            case 0 -> drawCross(ctx, cx, cy, size, gap, th, baseColor, outlineColor);
            case 1 -> drawDot(ctx, cx, cy, Math.max(1.5f, th), baseColor);
            case 2 -> drawRing(ctx, cx, cy, size + gap, th, baseColor);
            case 3 -> {
                drawCross(ctx, cx, cy, size, gap, th, baseColor, outlineColor);
                drawDot(ctx, cx, cy, 1.5f, baseColor);
            }
            case 4 -> drawChevron(ctx, cx, cy, size, gap, th, baseColor, outlineColor);
            default -> drawCross(ctx, cx, cy, size, gap, th, baseColor, outlineColor);
        }

        ctx.draw();
    }

    private static boolean isAimingAtEntity(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        return hit instanceof EntityHitResult eHit && eHit.getEntity() instanceof LivingEntity;
    }

    private static void drawCross(DrawContext ctx, int cx, int cy, int size, int gap, int th,
                                  int color, int outline) {
        int halfTh = th / 2;
        int x0 = cx - halfTh;
        int y0 = cy - halfTh;

        // Outline
        int oHalfTh = halfTh + 1;
        int ox0 = cx - oHalfTh;
        int oy0 = cy - oHalfTh;
        int oth = th + 2;
        int oGap = Math.max(0, gap - 1);
        int oSize = size + 1;

        ctx.fill(ox0, cy - oGap - oSize, ox0 + oth, cy - oGap, outline);
        ctx.fill(ox0, cy + oGap, ox0 + oth, cy + oGap + oSize, outline);
        ctx.fill(cx - oGap - oSize, oy0, cx - oGap, oy0 + oth, outline);
        ctx.fill(cx + oGap, oy0, cx + oGap + oSize, oy0 + oth, outline);

        // Core
        ctx.fill(x0, cy - gap - size, x0 + th, cy - gap, color);
        ctx.fill(x0, cy + gap, x0 + th, cy + gap + size, color);
        ctx.fill(cx - gap - size, y0, cx - gap, y0 + th, color);
        ctx.fill(cx + gap, y0, cx + gap + size, y0 + th, color);
    }

    private static void drawDot(DrawContext ctx, int cx, int cy, float radius, int color) {
        // Outline ring
        SquircleRenderer.circle(ctx, cx, cy, radius + 1.0f, 0x90000000);
        // Core dot
        SquircleRenderer.circle(ctx, cx, cy, radius, color);
    }

    private static void drawRing(DrawContext ctx, int cx, int cy, float radius, float th, int color) {
        // Subtle outline drop
        SquircleRenderer.border(ctx, cx - radius - 1, cy - radius - 1, (radius + 1) * 2, (radius + 1) * 2, radius + 1, th + 1.5f, 0x70000000);
        // Anti-aliased smooth reticle ring
        SquircleRenderer.border(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, th, color);
    }

    private static void drawChevron(DrawContext ctx, int cx, int cy, int size, int gap, int th, int color, int outline) {
        // Center precision pip
        SquircleRenderer.circle(ctx, cx, cy, 1.2f, color);

        // Tactical inverted V chevron
        int topY = cy - gap;
        int spread = size;

        for (int i = 0; i <= size; i++) {
            float t = i / (float) size;
            int xL = cx - (int) (t * spread);
            int xR = cx + (int) (t * spread);
            int y = topY - i;
            ctx.fill(xL - 1, y, xL + th + 1, y + 1, outline);
            ctx.fill(xR - th - 1, y, xR + 1, y + 1, outline);
            ctx.fill(xL, y, xL + th, y + 1, color);
            ctx.fill(xR - th, y, xR, y + 1, color);
        }
    }

    private static int parseColor(String hex) {
        try {
            return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}