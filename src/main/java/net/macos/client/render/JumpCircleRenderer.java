package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Aesthetic Shockwave Jump Circles on player jump/landing.
 * Expanding neon ground ring with smooth fadeout.
 */
public final class JumpCircleRenderer {

    private static final List<JumpRing> rings = new ArrayList<>();
    private static boolean wasOnGround = true;
    private static double lastGroundY = 0;

    private static class JumpRing {
        Vec3d center;
        long startTime;

        JumpRing(Vec3d center, long startTime) {
            this.center = center;
            this.startTime = startTime;
        }
    }

    private JumpCircleRenderer() {}

    public static void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            rings.clear();
            return;
        }
        if (!ConfigManager.INSTANCE.enableJumpCircle) {
            rings.clear();
            return;
        }

        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            lastGroundY = mc.player.getY();
        }

        // Detect jump
        if (wasOnGround && !onGround && mc.player.getVelocity().y > 0.045) {
            rings.add(new JumpRing(
                new Vec3d(mc.player.getX(), lastGroundY + 0.02, mc.player.getZ()),
                System.currentTimeMillis()
            ));
        }
        wasOnGround = onGround;

        // Remove rings older than 800ms
        long now = System.currentTimeMillis();
        rings.removeIf(r -> (now - r.startTime) > 800L);
    }

    public static void render3D(WorldRenderContext context) {
        if (!ConfigManager.INSTANCE.enableJumpCircle || rings.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d camPos = context.camera().getPos();
        long now = System.currentTimeMillis();

        int color = 0x00D4FF;
        try {
            color = Integer.parseInt(ConfigManager.INSTANCE.jumpCircleColor.replace("#", ""), 16);
        } catch (Exception ignored) {}

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive neon glow
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int points = 36;
        float step = (float) (2 * Math.PI / points);

        for (JumpRing ring : rings) {
            float progress = (float) (now - ring.startTime) / 800.0f;
            if (progress >= 1.0f) continue;

            float ease = 1.0f - (float) Math.pow(1.0 - progress, 3.0); // EaseOutCubic
            float radius = 0.25f + ease * 2.0f;
            float innerRadius = Math.max(0.01f, radius - 0.25f);
            float alpha = (1.0f - progress) * 0.55f;

            buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            for (int i = 0; i <= points; i++) {
                float angle = i * step;
                float cos = MathHelper.cos(angle);
                float sin = MathHelper.sin(angle);

                float outerX = (float) (ring.center.x + cos * radius);
                float outerZ = (float) (ring.center.z + sin * radius);
                float innerX = (float) (ring.center.x + cos * innerRadius);
                float innerZ = (float) (ring.center.z + sin * innerRadius);
                float y = (float) ring.center.y;

                buffer.vertex(mat, outerX, y, outerZ).color(r, g, b, 0.0f).next();
                buffer.vertex(mat, innerX, y, innerZ).color(r, g, b, alpha).next();
            }

            tessellator.draw();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
