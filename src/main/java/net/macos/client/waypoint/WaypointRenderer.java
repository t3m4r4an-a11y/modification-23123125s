package net.macos.client.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Liquid Glass Waypoint Tag renderer inspired by Phantom & Phobia.
 * Features:
 * - Sleek floating glass pill with rounded corners, drop shadow, and crisp border
 * - Colored waypoint badge with POI icon
 * - Clean title and distance text
 * - Downward pin pointing to the world position
 * - Screen-edge clamping with directional arrow when looking away
 */
public class WaypointRenderer {

    private static final List<Projected> projections = new ArrayList<>();

    private static class Projected {
        Waypoint wp;
        float screenX;
        float screenY;
        double dist;
        boolean isBehind;
        boolean isClamped;
        float clampAngle;
    }

    public static void updateProjections(MatrixStack matrices, Camera camera, float tickDelta) {
        projections.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!ConfigManager.INSTANCE.enableWaypoints) return;

        String dim = mc.world.getRegistryKey().getValue().toString();
        Vec3d camPos = camera.getPos();
        double renderDist = mc.options.getViewDistance().getValue() * 16.0;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Matrix4f projMat = RenderSystem.getProjectionMatrix();

        for (Waypoint wp : WaypointManager.getAll()) {
            if (!wp.dimension.equals(dim)) continue;

            double dist = wp.distanceTo(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (dist > renderDist) continue;

            matrices.push();
            matrices.translate(wp.x + 0.5 - camPos.x, wp.y + 1.2 - camPos.y, wp.z + 0.5 - camPos.z);
            Matrix4f modelView = matrices.peek().getPositionMatrix();

            Vector4f clip = new Vector4f(0, 0, 0, 1);
            modelView.transform(clip);
            projMat.transform(clip);
            matrices.pop();

            boolean behind = clip.w <= 0.001f;
            float ndcX = clip.x / Math.abs(clip.w);
            float ndcY = clip.y / Math.abs(clip.w);

            if (behind) {
                ndcX = -ndcX;
                ndcY = -ndcY;
            }

            float sx = (ndcX * 0.5f + 0.5f) * sw;
            float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * sh;

            Projected p = new Projected();
            p.wp = wp;
            p.dist = dist;
            p.isBehind = behind;

            // Screen boundary clamping with margin
            float margin = 32f;
            boolean outOfBounds = behind || sx < margin || sx > sw - margin || sy < margin || sy > sh - margin;

            if (outOfBounds) {
                p.isClamped = true;
                float cx = sw / 2f;
                float cy = sh / 2f;
                float dx = sx - cx;
                float dy = sy - cy;
                p.clampAngle = (float) Math.atan2(dy, dx);

                float halfW = cx - margin;
                float halfH = cy - margin;

                float absCos = Math.abs((float) Math.cos(p.clampAngle));
                float absSin = Math.abs((float) Math.sin(p.clampAngle));

                float scaleFactor;
                if (halfW * absSin <= halfH * absCos) {
                    scaleFactor = halfW / Math.max(0.001f, absCos);
                } else {
                    scaleFactor = halfH / Math.max(0.001f, absSin);
                }

                p.screenX = cx + (float) Math.cos(p.clampAngle) * scaleFactor;
                p.screenY = cy + (float) Math.sin(p.clampAngle) * scaleFactor;
            } else {
                p.isClamped = false;
                p.screenX = sx;
                p.screenY = sy;
            }

            projections.add(p);
        }
    }

    public static void render2D(DrawContext context) {
        if (!ConfigManager.INSTANCE.enableWaypoints) return;
        if (projections.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        for (Projected p : projections) {
            renderWaypointTag(context, mc, p);
        }
    }

    private static void renderWaypointTag(DrawContext ctx, MinecraftClient mc, Projected p) {
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();

        // Distance scale: keep readable at any distance
        float scale = p.isClamped ? 0.90f : (float) MathHelper.clamp(1.0 - p.dist / 160.0, 0.82, 1.05);
        matrices.translate(p.screenX, p.screenY, 0f);
        matrices.scale(scale, scale, 1.0f);

        String title = p.wp.name;
        String distText = (int) p.dist + "m";

        int titleW = mc.textRenderer.getWidth(title);
        int distW = mc.textRenderer.getWidth(distText);
        int textW = Math.max(titleW, distW);

        int iconSize = 14;
        int padX = 6;
        int cardW = padX + iconSize + 6 + textW + padX;
        int cardH = 22;

        int x = -cardW / 2;
        int y = p.isClamped ? -cardH / 2 : -cardH - 6;

        int color = p.wp.color | 0xFF000000;

        // 1. Drop shadow & Dark Liquid Glass Card
        net.macos.client.hud.glass.GlassRenderer.dropShadow(ctx, x, y, cardW, cardH, 10, 6, 0x60);
        net.macos.client.render.SquircleRenderer.fill(ctx, x, y, cardW, cardH, 7, 0xDD0D111A);
        net.macos.client.render.SquircleRenderer.border(ctx, x, y, cardW, cardH, 7, 1.0f, 0x30FFFFFF);
        net.macos.client.hud.glass.GlassRenderer.specular(ctx, x, y, cardW, cardH, 7, 0x25FFFFFF);

        // 2. Colored badge with POI icon on the left
        int badgeX = x + padX;
        int badgeY = y + (cardH - iconSize) / 2;
        net.macos.client.render.SquircleRenderer.pill(ctx, badgeX, badgeY, iconSize, iconSize, color);
        net.macos.client.gui.icon.IconRenderer.draw(ctx, net.macos.client.gui.icon.MacIcons.LOCATION, badgeX + 1, badgeY + 1, 0xFFFFFFFF);

        // 3. Texts
        int textX = badgeX + iconSize + 5;
        // Waypoint name (crisp white)
        net.macos.client.gui.font.AetherionFont.draw(ctx, title, textX, y + 3, 0xFFFFFFFF);
        // Distance (soft cyan / muted)
        net.macos.client.gui.font.AetherionFont.draw(ctx, distText, textX, y + 12, 0xCC00D4FF);

        // 4. Direction indicator
        if (p.isClamped) {
            // Arrow indicator pointing offscreen
            drawArrow(ctx, 0, 0, p.clampAngle, color);
        } else {
            // Pin pointer pointing down to the block
            net.macos.client.render.SquircleRenderer.pill(ctx, -1, y + cardH, 2, 4, color);
        }

        matrices.pop();
    }

    private static void drawArrow(DrawContext ctx, int x, int y, float angle, int color) {
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(angle));

        // 3-pixel sharp directional pointer
        ctx.fill(16, -3, 20, 3, color);
        ctx.fill(20, -2, 23, 2, color);
        ctx.fill(23, -1, 25, 1, color);

        matrices.pop();
    }
}