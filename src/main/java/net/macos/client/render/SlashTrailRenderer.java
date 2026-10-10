package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Smooth, subtle Weapon Slash Trails inspired by anime & action RPG combat.
 * Lightweight, short-decaying (240ms) ribbon trail behind sword swings without screen clutter.
 */
public final class SlashTrailRenderer {

    private static final List<TrailSegment> segments = new ArrayList<>();
    private static float lastSwingProgress = 0f;

    private static class TrailSegment {
        Vec3d tip;
        Vec3d base;
        long time;

        TrailSegment(Vec3d tip, Vec3d base, long time) {
            this.tip = tip;
            this.base = base;
            this.time = time;
        }
    }

    private SlashTrailRenderer() {}

    public static void onClientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            segments.clear();
            return;
        }
        if (!ConfigManager.INSTANCE.enableSlashTrails) {
            segments.clear();
            return;
        }

        PlayerEntity player = mc.player;
        float swing = player.getHandSwingProgress(1.0f);

        // Record sword blade arc during swing
        boolean isSwinging = player.handSwinging && swing > 0.01f && swing < 0.95f;
        boolean hasWeapon = player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof SwordItem;

        if (isSwinging && hasWeapon) {
            Vec3d eyePos = player.getCameraPosVec(1.0f);
            Vec3d look = player.getRotationVec(1.0f);

            // Compute blade arc orientation based on swing angle
            float angle = (swing - 0.5f) * 1.8f;
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);

            Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
            Vec3d up = right.crossProduct(look).normalize();

            Vec3d bladeDir = look.multiply(0.4).add(right.multiply(sin * 0.9)).add(up.multiply(-cos * 0.7));
            Vec3d basePos = eyePos.add(look.multiply(0.3)).add(right.multiply(0.2)).add(up.multiply(-0.2));
            Vec3d tipPos  = basePos.add(bladeDir.normalize().multiply(1.15));

            segments.add(new TrailSegment(tipPos, basePos, System.currentTimeMillis()));
        }
        lastSwingProgress = swing;

        // Expire old segments
        long now = System.currentTimeMillis();
        long maxLife = ConfigManager.INSTANCE.slashTrailLifetime;
        segments.removeIf(s -> (now - s.time) > maxLife);
    }

    public static void render3D(WorldRenderContext context) {
        if (!ConfigManager.INSTANCE.enableSlashTrails || segments.size() < 2) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d camPos = context.camera().getPos();
        long now = System.currentTimeMillis();
        long maxLife = ConfigManager.INSTANCE.slashTrailLifetime;

        boolean isRainbow = "Rainbow".equalsIgnoreCase(ConfigManager.INSTANCE.slashTrailColor);
        boolean isAccent = "Accent".equalsIgnoreCase(ConfigManager.INSTANCE.slashTrailColor);

        int baseColor = 0x00D4FF;
        if (isAccent) {
            try {
                baseColor = Integer.parseInt(ConfigManager.INSTANCE.accentColor.replace("#", ""), 16);
            } catch (Exception ignored) {}
        } else if (!isRainbow) {
            try {
                baseColor = Integer.parseInt(ConfigManager.INSTANCE.slashTrailColor.replace("#", ""), 16);
            } catch (Exception ignored) {}
        }

        float r = ((baseColor >> 16) & 0xFF) / 255.0f;
        float g = ((baseColor >> 8) & 0xFF) / 255.0f;
        float b = (baseColor & 0xFF) / 255.0f;

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive soft neon glow
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments.size(); i++) {
            TrailSegment seg = segments.get(i);
            float age = (float) (now - seg.time) / maxLife;
            float alpha = MathHelper.clamp(1.0f - age, 0.0f, 1.0f) * 0.65f;

            float segR = r, segG = g, segB = b;
            if (isRainbow) {
                float hue = ((now + i * 40L) % 2500L) / 2500.0f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.95f, 1.0f);
                segR = ((rgb >> 16) & 0xFF) / 255.0f;
                segG = ((rgb >> 8) & 0xFF) / 255.0f;
                segB = (rgb & 0xFF) / 255.0f;
            }

            // Tip (bright outer edge)
            buffer.vertex(mat, (float) seg.tip.x, (float) seg.tip.y, (float) seg.tip.z)
                  .color(segR, segG, segB, alpha)
                  .next();
            // Base (glowing inner spine)
            buffer.vertex(mat, (float) seg.base.x, (float) seg.base.y, (float) seg.base.z)
                  .color(Math.min(1f, segR * 0.7f + 0.3f), Math.min(1f, segG * 0.7f + 0.3f), Math.min(1f, segB * 0.7f + 0.3f), 0.06f)
                  .next();
        }

        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
