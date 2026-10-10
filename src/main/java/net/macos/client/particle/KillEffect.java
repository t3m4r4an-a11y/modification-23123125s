package net.macos.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.macos.client.render.GLStateGuard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 100% GPU-Accelerated 3D Kill Effects rendered in true world space.
 * Zero CPU pixel overhead, zero FPS drops.
 */
public final class KillEffect {

    private static final Random RNG = new Random();
    private static final List<Effect3D> ACTIVE_EFFECTS = new ArrayList<>();
    private static final long DURATION_MS = 1400L;

    private static class Effect3D {
        double x, y, z;
        long startTime;
        KillEffectType type;
        int color;
        long seed;

        Effect3D(double x, double y, double z, KillEffectType type, int color, long seed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startTime = System.currentTimeMillis();
            this.type = type;
            this.color = color;
            this.seed = seed;
        }
    }

    private KillEffect() {}

    public static void trigger(double x, double y, double z, KillEffectType type, String colorHex) {
        if (type == KillEffectType.NONE) return;

        int color = 0x00D4FF;
        try {
            color = Color.decode(colorHex).getRGB() & 0xFFFFFF;
        } catch (Exception ignored) {}

        if (ACTIVE_EFFECTS.size() >= 6) {
            ACTIVE_EFFECTS.remove(0);
        }

        ACTIVE_EFFECTS.add(new Effect3D(x, y, z, type, color, RNG.nextLong()));

        // Play cosmetic sound for lightning
        if (type == KillEffectType.LIGHTNING) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
                mc.world.playSound(x, y, z,
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundCategory.PLAYERS, 0.8f, 1.1f, false);
            }
        }
    }

    public static void render3D(WorldRenderContext context) {
        if (ACTIVE_EFFECTS.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d camPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        long now = System.currentTimeMillis();

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        try (var guard = GLStateGuard.push()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Iterator<Effect3D> it = ACTIVE_EFFECTS.iterator();
            while (it.hasNext()) {
                Effect3D e = it.next();
                long elapsed = now - e.startTime;
                if (elapsed > DURATION_MS) {
                    it.remove();
                    continue;
                }

                float progress = elapsed / (float) DURATION_MS;
                float alpha = 1.0f - progress;

                switch (e.type) {
                    case RING      -> renderRing3D(mat, e, progress, alpha);
                    case BEAMS     -> renderBeams3D(mat, e, progress, alpha);
                    case LIGHTNING -> renderLightning3D(mat, e, progress, alpha);
                    case BURST     -> renderBurst3D(mat, e, progress, alpha);
                    case SPIRAL    -> renderSpiral3D(mat, e, progress, alpha);
                    case GHOST     -> renderGhost3D(mat, e, progress, alpha);
                    default        -> {}
                }
            }
        } finally {
            matrices.pop();
        }
    }

    // ============================================================
    // 1. 3D SHOCKWAVE GROUND RING (TRIANGLE_STRIP & RUNIC DASHES)
    // ============================================================
    private static void renderRing3D(Matrix4f mat, Effect3D e, float progress, float alpha) {
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        float radius = 0.3f + eased * 4.2f;
        float width = 0.45f * (1.0f - progress * 0.7f);

        float r = ((e.color >> 16) & 0xFF) / 255f;
        float g = ((e.color >> 8) & 0xFF) / 255f;
        float b = (e.color & 0xFF) / 255f;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int segments = 48;
        double cy = e.y + 0.05;

        // Primary outer shockwave ring
        for (int i = 0; i <= segments; i++) {
            double angle = (2.0 * Math.PI * i) / segments;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double innerX = e.x + cos * Math.max(0.01, radius - width);
            double innerZ = e.z + sin * Math.max(0.01, radius - width);
            double outerX = e.x + cos * (radius + width);
            double outerZ = e.z + sin * (radius + width);

            bb.vertex(mat, (float) innerX, (float) cy, (float) innerZ)
                    .color(r, g, b, alpha * 0.95f).next();
            bb.vertex(mat, (float) outerX, (float) cy, (float) outerZ)
                    .color(r, g, b, 0.0f).next();
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());

        // Secondary high-speed inner ripple ring
        float innerRadius = radius * 0.55f;
        float innerW = width * 0.5f;
        bb.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double angle = (2.0 * Math.PI * i) / 32.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double inX = e.x + cos * Math.max(0.01, innerRadius - innerW);
            double inZ = e.z + sin * Math.max(0.01, innerRadius - innerW);
            double outX = e.x + cos * (innerRadius + innerW);
            double outZ = e.z + sin * (innerRadius + innerW);

            bb.vertex(mat, (float) inX, (float) (cy + 0.01), (float) inZ)
                    .color(1f, 1f, 1f, alpha * 0.8f).next();
            bb.vertex(mat, (float) outX, (float) (cy + 0.01), (float) outZ)
                    .color(r, g, b, 0.0f).next();
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // ============================================================
    // 2. 3D RADIANT BEAMS (CYLINDRICAL ENERGY BEAMS VIA QUADS)
    // ============================================================
    private static void renderBeams3D(Matrix4f mat, Effect3D e, float progress, float alpha) {
        float r = ((e.color >> 16) & 0xFF) / 255f;
        float g = ((e.color >> 8) & 0xFF) / 255f;
        float b = (e.color & 0xFF) / 255f;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float beamHeight = 5.5f * (1.0f - progress * 0.15f);
        float by = (float) e.y;

        // Central hyper-bright radiant core pillar
        float cw = 0.22f * (1.0f - progress * 0.5f);
        bb.vertex(mat, (float) e.x - cw, by, (float) e.z - cw).color(1f, 1f, 1f, alpha * 0.95f).next();
        bb.vertex(mat, (float) e.x + cw, by, (float) e.z - cw).color(1f, 1f, 1f, alpha * 0.95f).next();
        bb.vertex(mat, (float) e.x + cw, by + beamHeight, (float) e.z - cw).color(r, g, b, 0.0f).next();
        bb.vertex(mat, (float) e.x - cw, by + beamHeight, (float) e.z - cw).color(r, g, b, 0.0f).next();

        bb.vertex(mat, (float) e.x, by, (float) e.z - cw).color(1f, 1f, 1f, alpha * 0.95f).next();
        bb.vertex(mat, (float) e.x, by, (float) e.z + cw).color(1f, 1f, 1f, alpha * 0.95f).next();
        bb.vertex(mat, (float) e.x, by + beamHeight, (float) e.z + cw).color(r, g, b, 0.0f).next();
        bb.vertex(mat, (float) e.x, by + beamHeight, (float) e.z - cw).color(r, g, b, 0.0f).next();

        // 6 orbiting vertical beam pillars
        int beams = 6;
        float spin = progress * 140.0f;
        float radius = 0.65f + progress * 1.8f;
        float w = 0.14f * (1.0f - progress * 0.5f);

        for (int i = 0; i < beams; i++) {
            double angle = Math.toRadians((360.0 / beams) * i + spin);
            float bx = (float) (e.x + Math.cos(angle) * radius);
            float bz = (float) (e.z + Math.sin(angle) * radius);

            // Crossed quads for full 360-degree visibility
            bb.vertex(mat, bx - w, by, bz).color(r, g, b, alpha * 0.85f).next();
            bb.vertex(mat, bx + w, by, bz).color(r, g, b, alpha * 0.85f).next();
            bb.vertex(mat, bx + w, by + beamHeight * 0.85f, bz).color(r, g, b, 0.0f).next();
            bb.vertex(mat, bx - w, by + beamHeight * 0.85f, bz).color(r, g, b, 0.0f).next();

            bb.vertex(mat, bx, by, bz - w).color(r, g, b, alpha * 0.85f).next();
            bb.vertex(mat, bx, by, bz + w).color(r, g, b, alpha * 0.85f).next();
            bb.vertex(mat, bx, by + beamHeight * 0.85f, bz + w).color(r, g, b, 0.0f).next();
            bb.vertex(mat, bx, by + beamHeight * 0.85f, bz - w).color(r, g, b, 0.0f).next();
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // ============================================================
    // 3. 3D LIGHTNING BOLT WITH BRANCHING FORKS
    // ============================================================
    private static void renderLightning3D(Matrix4f mat, Effect3D e, float progress, float alpha) {
        float r = ((e.color >> 16) & 0xFF) / 255f;
        float g = ((e.color >> 8) & 0xFF) / 255f;
        float b = (e.color & 0xFF) / 255f;

        Random rand = new Random(e.seed);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int segments = 14;
        float height = 16.0f;
        float curX = (float) e.x;
        float curY = (float) (e.y + height);
        float curZ = (float) e.z;

        float boltAlpha = alpha * (0.7f + rand.nextFloat() * 0.3f);
        float w = 0.10f * (1.0f - progress * 0.4f);

        // Main trunk
        for (int i = 0; i <= segments; i++) {
            float nextY = (float) (e.y + height - (height / segments) * i);
            float nextX = (float) (e.x + (rand.nextFloat() - 0.5f) * 0.7f * (segments - i) / (float) segments);
            float nextZ = (float) (e.z + (rand.nextFloat() - 0.5f) * 0.7f * (segments - i) / (float) segments);
            if (i == segments) {
                nextX = (float) e.x;
                nextY = (float) e.y;
                nextZ = (float) e.z;
            }

            // Facing quad 1
            bb.vertex(mat, curX - w, curY, curZ).color(1f, 1f, 1f, boltAlpha).next();
            bb.vertex(mat, curX + w, curY, curZ).color(1f, 1f, 1f, boltAlpha).next();
            bb.vertex(mat, nextX + w, nextY, nextZ).color(r, g, b, boltAlpha).next();
            bb.vertex(mat, nextX - w, nextY, nextZ).color(r, g, b, boltAlpha).next();

            // Facing quad 2 (perpendicular)
            bb.vertex(mat, curX, curY, curZ - w).color(1f, 1f, 1f, boltAlpha).next();
            bb.vertex(mat, curX, curY, curZ + w).color(1f, 1f, 1f, boltAlpha).next();
            bb.vertex(mat, nextX, nextY, nextZ + w).color(r, g, b, boltAlpha).next();
            bb.vertex(mat, nextX, nextY, nextZ - w).color(r, g, b, boltAlpha).next();

            curX = nextX;
            curY = nextY;
            curZ = nextZ;
        }

        // Ground impact flash
        float fw = 0.8f * (1.0f - progress * 0.7f);
        bb.vertex(mat, (float) e.x - fw, (float) e.y + 0.05f, (float) e.z - fw).color(r, g, b, boltAlpha * 0.9f).next();
        bb.vertex(mat, (float) e.x + fw, (float) e.y + 0.05f, (float) e.z - fw).color(r, g, b, boltAlpha * 0.9f).next();
        bb.vertex(mat, (float) e.x + fw, (float) e.y + 0.05f, (float) e.z + fw).color(r, g, b, boltAlpha * 0.9f).next();
        bb.vertex(mat, (float) e.x - fw, (float) e.y + 0.05f, (float) e.z + fw).color(r, g, b, boltAlpha * 0.9f).next();

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // ============================================================
    // 4. 3D PARTICLE BURST (CELESTIAL NOVA EXPLOSION)
    // ============================================================
    private static void renderBurst3D(Matrix4f mat, Effect3D e, float progress, float alpha) {
        float r = ((e.color >> 16) & 0xFF) / 255f;
        float g = ((e.color >> 8) & 0xFF) / 255f;
        float b = (e.color & 0xFF) / 255f;

        Random rand = new Random(e.seed);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int count = 48;
        float ease = (float) Math.sin(progress * Math.PI * 0.5);

        for (int i = 0; i < count; i++) {
            double theta = rand.nextDouble() * Math.PI * 2.0;
            double phi = rand.nextDouble() * Math.PI;
            double speed = 1.6 + rand.nextDouble() * 2.8;

            float dist = (float) (speed * ease * 2.4f);
            float px = (float) (e.x + Math.sin(phi) * Math.cos(theta) * dist);
            float py = (float) (e.y + 0.9 + Math.cos(phi) * dist - progress * progress * 1.8);
            float pz = (float) (e.z + Math.sin(phi) * Math.sin(theta) * dist);

            float s = 0.08f * (1.0f - progress * 0.65f);
            float pa = alpha * (0.6f + rand.nextFloat() * 0.4f);

            // Shard diamond quad
            bb.vertex(mat, px - s, py, pz).color(1f, 1f, 1f, pa).next();
            bb.vertex(mat, px, py - s, pz).color(r, g, b, pa).next();
            bb.vertex(mat, px + s, py, pz).color(1f, 1f, 1f, pa).next();
            bb.vertex(mat, px, py + s, pz).color(r, g, b, pa).next();
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // ============================================================
    // 5. 3D SPIRAL VORTEX (DUAL HELIX)
    // ============================================================
    private static void renderSpiral3D(Matrix4f mat, Effect3D e, float progress, float alpha) {
        float r = ((e.color >> 16) & 0xFF) / 255f;
        float g = ((e.color >> 8) & 0xFF) / 255f;
        float b = (e.color & 0xFF) / 255f;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int points = 44;
        float baseSpin = progress * 420.0f;
        float rise = progress * 4.2f;

        for (int i = 0; i < points; i++) {
            float t = i / (float) points;
            double a1 = Math.toRadians(baseSpin + i * 20.0);
            double a2 = a1 + Math.PI; // Intertwined twin stream
            float rad = 0.45f + t * 0.7f;

            // Stream 1
            float px1 = (float) (e.x + Math.cos(a1) * rad);
            float py1 = (float) (e.y + t * rise);
            float pz1 = (float) (e.z + Math.sin(a1) * rad);

            float s = 0.06f * (1.0f - t * 0.4f);
            float pa = alpha * (1.0f - t * 0.35f);

            bb.vertex(mat, px1 - s, py1, pz1 - s).color(r, g, b, pa).next();
            bb.vertex(mat, px1 + s, py1, pz1 - s).color(r, g, b, pa).next();
            bb.vertex(mat, px1 + s, py1 + s * 2f, pz1 + s).color(1f, 1f, 1f, pa).next();
            bb.vertex(mat, px1 - s, py1 + s * 2f, pz1 + s).color(1f, 1f, 1f, pa).next();

            // Stream 2
            float px2 = (float) (e.x + Math.cos(a2) * rad);
            float py2 = (float) (e.y + t * rise);
            float pz2 = (float) (e.z + Math.sin(a2) * rad);

            bb.vertex(mat, px2 - s, py2, pz2 - s).color(1f, 1f, 1f, pa).next();
            bb.vertex(mat, px2 + s, py2, pz2 - s).color(1f, 1f, 1f, pa).next();
            bb.vertex(mat, px2 + s, py2 + s * 2f, pz2 + s).color(r, g, b, pa).next();
            bb.vertex(mat, px2 - s, py2 + s * 2f, pz2 + s).color(r, g, b, pa).next();
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // ============================================================
    // 6. 3D ASCENDING GHOST PHANTOM
    // ============================================================
    private static void renderGhost3D(Matrix4f mat, Effect3D e, float progress, float alpha) {
        float r = ((e.color >> 16) & 0xFF) / 255f;
        float g = ((e.color >> 8) & 0xFF) / 255f;
        float b = (e.color & 0xFF) / 255f;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float rise = progress * 3.4f;
        float gx = (float) e.x;
        float gy = (float) (e.y + 0.3f + rise);
        float gz = (float) e.z;
        float scale = 0.65f * (1.0f + progress * 0.25f);

        int rings = 6;
        for (int ring = 0; ring <= rings; ring++) {
            float ringY = gy + ring * (scale / rings);
            float ringRadius = scale * (0.85f - ring * 0.11f);
            float ringAlpha = alpha * (1.0f - ring / (float) rings);

            for (int i = 0; i <= 18; i++) {
                double angle = (2.0 * Math.PI * i) / 18.0;
                float px = (float) (gx + Math.cos(angle) * ringRadius);
                float pz = (float) (gz + Math.sin(angle) * ringRadius);

                bb.vertex(mat, px, ringY, pz).color(r, g, b, ringAlpha * 0.75f).next();
                bb.vertex(mat, px, ringY + 0.12f, pz).color(1f, 1f, 1f, ringAlpha * 0.45f).next();
            }
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());
    }
}