package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Aesthetic 2026 3D Impact Bubble FX when striking entities:
 * 1. Radiant luminous core with smooth cubic ease-out swelling.
 * 2. High-energy chromatic shockwave perimeter ring.
 * 3. Delicate micro-starburst sparks radiating on impact.
 */
public final class HitBubbleRenderer {

    private static final List<Bubble> bubbles = new ArrayList<>();

    private static class Bubble {
        final Vec3d pos;
        final long spawnTime;

        Bubble(Vec3d pos, long spawnTime) {
            this.pos = pos;
            this.spawnTime = spawnTime;
        }
    }

    private HitBubbleRenderer() {}

    public static void onEntityHit(Entity target) {
        if (!ConfigManager.INSTANCE.enableHitBubble || target == null) return;
        Vec3d pos = target.getPos().add(0, target.getHeight() * 0.65, 0);
        bubbles.add(new Bubble(pos, System.currentTimeMillis()));
    }

    public static void render3D(WorldRenderContext context) {
        if (!ConfigManager.INSTANCE.enableHitBubble || bubbles.isEmpty()) return;

        long now = System.currentTimeMillis();
        bubbles.removeIf(b -> (now - b.spawnTime) > 550L);
        if (bubbles.isEmpty()) return;

        Vec3d camPos = context.camera().getPos();
        float[] rgb = getBubbleRgb();

        MatrixStack matrices = context.matrixStack();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive luminous blend
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        for (Bubble bubble : bubbles) {
            float progress = (float) (now - bubble.spawnTime) / 550.0f;
            if (progress >= 1.0f) continue;

            // Ease-out cubic curve for punchy impact pop
            float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
            float scale = 0.15f + ease * 0.50f;
            float alpha = (1.0f - progress);

            matrices.push();
            matrices.translate(bubble.pos.x - camPos.x, bubble.pos.y - camPos.y + progress * 0.18, bubble.pos.z - camPos.z);
            matrices.multiply(context.camera().getRotation()); // Billboard facing camera
            matrices.scale(scale, scale, scale);

            Matrix4f mat = matrices.peek().getPositionMatrix();

            // ── Pass 1: Soft Luminous Core (Fan) ───────────────────────────
            buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(mat, 0f, 0f, 0f).color(1f, 1f, 1f, alpha * 0.95f).next(); // Pure white hot core

            int segments = 28;
            float step = (float) (2 * Math.PI / segments);
            for (int i = 0; i <= segments; i++) {
                float a = i * step;
                buffer.vertex(mat, MathHelper.cos(a) * 0.65f, MathHelper.sin(a) * 0.65f, 0f)
                      .color(rgb[0], rgb[1], rgb[2], 0.0f)
                      .next();
            }
            tessellator.draw();

            // ── Pass 2: High-Energy Perimeter Ring (Strip) ─────────────────
            float ringInner = 0.72f;
            float ringOuter = 0.88f;
            float ringAlpha = (float) Math.sin(progress * Math.PI) * 0.85f;

            buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                float a = i * step;
                float ca = MathHelper.cos(a);
                float sa = MathHelper.sin(a);

                buffer.vertex(mat, ca * ringOuter, sa * ringOuter, 0f)
                      .color(rgb[0], rgb[1], rgb[2], 0.0f)
                      .next();
                buffer.vertex(mat, ca * ringInner, sa * ringInner, 0f)
                      .color(Math.min(1f, rgb[0] * 1.25f), Math.min(1f, rgb[1] * 1.25f), Math.min(1f, rgb[2] * 1.25f), ringAlpha)
                      .next();
            }
            tessellator.draw();

            // ── Pass 3: 4-Point Starburst Spark ────────────────────────────
            float sparkLen = 1.05f * (1.0f - progress * 0.6f);
            float sparkW = 0.08f * (1.0f - progress);
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            // Horizontal ray
            buffer.vertex(mat, -sparkLen, -sparkW, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            buffer.vertex(mat,  sparkLen, -sparkW, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            buffer.vertex(mat,  sparkLen,  sparkW, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            buffer.vertex(mat, -sparkLen,  sparkW, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            // Vertical ray
            buffer.vertex(mat, -sparkW, -sparkLen, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            buffer.vertex(mat,  sparkW, -sparkLen, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            buffer.vertex(mat,  sparkW,  sparkLen, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            buffer.vertex(mat, -sparkW,  sparkLen, 0f).color(rgb[0], rgb[1], rgb[2], 0f).next();
            tessellator.draw();

            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static float[] getBubbleRgb() {
        String hex = ConfigManager.INSTANCE.hitBubbleColor;
        if (hex == null || hex.isEmpty()) hex = ConfigManager.INSTANCE.accentColor;
        try {
            int c = Integer.parseInt(hex.replace("#", ""), 16);
            return new float[]{
                ((c >> 16) & 0xFF) / 255.0f,
                ((c >> 8)  & 0xFF) / 255.0f,
                (c         & 0xFF) / 255.0f
            };
        } catch (Exception e) {
            return new float[]{ 0.0f, 0.83f, 1.0f }; // Luxury Cyan default
        }
    }
}
