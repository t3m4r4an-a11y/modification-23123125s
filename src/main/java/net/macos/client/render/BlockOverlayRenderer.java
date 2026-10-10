package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.macos.client.config.ConfigManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * 3D Procedural Block Overlay renderer for Aetherion 2026.
 * Features:
 * - Ultra-smooth position lerping between targeted blocks.
 * - Dual-layer outline rendering: OpenGL DEBUG_LINES + 3D distance-adaptive quad ribbons for 100% visibility.
 * - Dynamic animated gradient patterns (Rainbow, Cyber, Aurora, Fire, Plasma, Glitch, Pulse, Normal).
 * - Independent Fill and Outline controls with zero Z-fighting.
 */
public final class BlockOverlayRenderer {

    private BlockOverlayRenderer() {}

    private static Box currentBox = null;
    private static BlockPos lastPos = null;

    public static void render3D(WorldRenderContext context) {
        ConfigManager cfg = ConfigManager.INSTANCE;
        if (!cfg.enableBlockOverlay) {
            currentBox = null;
            lastPos = null;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) {
            currentBox = null;
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            currentBox = null;
            return;
        }

        if (hit.getType() == HitResult.Type.MISS) {
            currentBox = null;
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            currentBox = null;
            return;
        }

        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) {
            currentBox = null;
            return;
        }

        Box targetBox = shape.getBoundingBox().offset(pos.getX(), pos.getY(), pos.getZ());

        // Smooth box interpolation
        if ("Smooth".equalsIgnoreCase(cfg.blockOverlayMode)) {
            if (currentBox == null || lastPos == null || !lastPos.equals(pos)) {
                if (currentBox == null) currentBox = targetBox;
            }
            float speed = 0.28f;
            currentBox = new Box(
                    MathHelper.lerp(speed, currentBox.minX, targetBox.minX),
                    MathHelper.lerp(speed, currentBox.minY, targetBox.minY),
                    MathHelper.lerp(speed, currentBox.minZ, targetBox.minZ),
                    MathHelper.lerp(speed, currentBox.maxX, targetBox.maxX),
                    MathHelper.lerp(speed, currentBox.maxY, targetBox.maxY),
                    MathHelper.lerp(speed, currentBox.maxZ, targetBox.maxZ)
            );
        } else {
            currentBox = targetBox;
        }
        lastPos = pos;

        renderBox(context, currentBox);
    }

    private static void renderBox(WorldRenderContext context, Box box) {
        ConfigManager cfg = ConfigManager.INSTANCE;
        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        int baseColor = parseHex(cfg.blockOverlayColor);
        float[][] patternColors = computePatternColors(cfg.blockOverlayShader, baseColor);
        float[] cTop = patternColors[0];
        float[] cBot = patternColors[1];

        double distToCamera = Math.sqrt(box.squaredMagnitude(cameraPos));

        try (var guard = GLStateGuard.push()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            // Expand slightly outside the block geometry to eliminate Z-fighting
            Box renderBox = box.expand(0.002);
            float minX = (float) renderBox.minX;
            float minY = (float) renderBox.minY;
            float minZ = (float) renderBox.minZ;
            float maxX = (float) renderBox.maxX;
            float maxY = (float) renderBox.maxY;
            float maxZ = (float) renderBox.maxZ;

            // 1. Fill Faces
            if (cfg.blockOverlayFill && cfg.blockOverlayFillAlpha > 0) {
                float fa = (cfg.blockOverlayFillAlpha / 255.0f);
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                BufferBuilder bb = Tessellator.getInstance().getBuffer();
                bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                // Bottom face (cBot)
                bb.vertex(mat, minX, minY, minZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, maxX, minY, minZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, maxX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, minX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], fa).next();

                // Top face (cTop)
                bb.vertex(mat, minX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, minX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, maxX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, maxX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], fa).next();

                // North face (Z-)
                bb.vertex(mat, minX, minY, minZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, minX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, maxX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, maxX, minY, minZ).color(cBot[0], cBot[1], cBot[2], fa).next();

                // South face (Z+)
                bb.vertex(mat, minX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, maxX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, maxX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, minX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], fa).next();

                // West face (X-)
                bb.vertex(mat, minX, minY, minZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, minX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, minX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, minX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], fa).next();

                // East face (X+)
                bb.vertex(mat, maxX, minY, minZ).color(cBot[0], cBot[1], cBot[2], fa).next();
                bb.vertex(mat, maxX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, maxX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], fa).next();
                bb.vertex(mat, maxX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], fa).next();

                BufferRenderer.drawWithGlobalProgram(bb.end());
            }

            // 2. Outline Lines (DEBUG_LINES + Adaptive 3D Quads)
            if (cfg.blockOverlayOutline) {
                int lineAlphaInt = cfg.blockOverlayLineAlpha > 0 ? cfg.blockOverlayLineAlpha : 220;
                float la = lineAlphaInt / 255.0f;

                // Pass A: Native OpenGL Lines (Crisp screen-space wireframe)
                float lineWidth = Math.max(1.0f, cfg.blockOverlayLineWidth);
                GL11.glLineWidth(lineWidth);

                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                BufferBuilder bbLines = Tessellator.getInstance().getBuffer();
                bbLines.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                // Bottom 4 lines
                bbLines.vertex(mat, minX, minY, minZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, maxX, minY, minZ).color(cBot[0], cBot[1], cBot[2], la).next();

                bbLines.vertex(mat, maxX, minY, minZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, maxX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], la).next();

                bbLines.vertex(mat, maxX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, minX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], la).next();

                bbLines.vertex(mat, minX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, minX, minY, minZ).color(cBot[0], cBot[1], cBot[2], la).next();

                // Top 4 lines
                bbLines.vertex(mat, minX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], la).next();
                bbLines.vertex(mat, maxX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], la).next();

                bbLines.vertex(mat, maxX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], la).next();
                bbLines.vertex(mat, maxX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], la).next();

                bbLines.vertex(mat, maxX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], la).next();
                bbLines.vertex(mat, minX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], la).next();

                bbLines.vertex(mat, minX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], la).next();
                bbLines.vertex(mat, minX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], la).next();

                // 4 Vertical Pillars
                bbLines.vertex(mat, minX, minY, minZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, minX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], la).next();

                bbLines.vertex(mat, maxX, minY, minZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, maxX, maxY, minZ).color(cTop[0], cTop[1], cTop[2], la).next();

                bbLines.vertex(mat, maxX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, maxX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], la).next();

                bbLines.vertex(mat, minX, minY, maxZ).color(cBot[0], cBot[1], cBot[2], la).next();
                bbLines.vertex(mat, minX, maxY, maxZ).color(cTop[0], cTop[1], cTop[2], la).next();

                BufferRenderer.drawWithGlobalProgram(bbLines.end());

                // Pass B: Thick 3D Quad Ribbons (for GPUs where GL lines are thin, distance-scaled)
                float thick = (float) Math.max(0.012f, Math.min(0.045f, distToCamera * 0.0035f * (lineWidth / 2.0f)));

                BufferBuilder bbRibbons = Tessellator.getInstance().getBuffer();
                bbRibbons.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                // 4 Bottom edges
                drawThickEdgeX(bbRibbons, mat, minX, maxX, minY, minZ, thick, cBot, la);
                drawThickEdgeX(bbRibbons, mat, minX, maxX, minY, maxZ, thick, cBot, la);
                drawThickEdgeZ(bbRibbons, mat, minX, minY, minZ, maxZ, thick, cBot, la);
                drawThickEdgeZ(bbRibbons, mat, maxX, minY, minZ, maxZ, thick, cBot, la);

                // 4 Top edges
                drawThickEdgeX(bbRibbons, mat, minX, maxX, maxY, minZ, thick, cTop, la);
                drawThickEdgeX(bbRibbons, mat, minX, maxX, maxY, maxZ, thick, cTop, la);
                drawThickEdgeZ(bbRibbons, mat, minX, maxY, minZ, maxZ, thick, cTop, la);
                drawThickEdgeZ(bbRibbons, mat, maxX, maxY, minZ, maxZ, thick, cTop, la);

                // 4 Vertical corner pillars
                drawThickEdgeY(bbRibbons, mat, minX, minZ, minY, maxY, thick, cBot, cTop, la);
                drawThickEdgeY(bbRibbons, mat, maxX, minZ, minY, maxY, thick, cBot, cTop, la);
                drawThickEdgeY(bbRibbons, mat, minX, maxZ, minY, maxY, thick, cBot, cTop, la);
                drawThickEdgeY(bbRibbons, mat, maxX, maxZ, minY, maxY, thick, cBot, cTop, la);

                BufferRenderer.drawWithGlobalProgram(bbRibbons.end());
            }
        } finally {
            matrices.pop();
        }
    }

    private static void drawThickEdgeX(BufferBuilder bb, Matrix4f mat, float x1, float x2, float y, float z, float t, float[] c, float a) {
        // Horizontal ribbon (facing Y)
        bb.vertex(mat, x1, y, z - t).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x2, y, z - t).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x2, y, z + t).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x1, y, z + t).color(c[0], c[1], c[2], a).next();
        // Vertical ribbon (facing Z)
        bb.vertex(mat, x1, y - t, z).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x2, y - t, z).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x2, y + t, z).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x1, y + t, z).color(c[0], c[1], c[2], a).next();
    }

    private static void drawThickEdgeZ(BufferBuilder bb, Matrix4f mat, float x, float y, float z1, float z2, float t, float[] c, float a) {
        // Horizontal ribbon (facing Y)
        bb.vertex(mat, x - t, y, z1).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x + t, y, z1).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x + t, y, z2).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x - t, y, z2).color(c[0], c[1], c[2], a).next();
        // Vertical ribbon (facing X)
        bb.vertex(mat, x, y - t, z1).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x, y - t, z2).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x, y + t, z2).color(c[0], c[1], c[2], a).next();
        bb.vertex(mat, x, y + t, z1).color(c[0], c[1], c[2], a).next();
    }

    private static void drawThickEdgeY(BufferBuilder bb, Matrix4f mat, float x, float z, float y1, float y2, float t, float[] c1, float[] c2, float a) {
        // Facing Z
        bb.vertex(mat, x - t, y1, z).color(c1[0], c1[1], c1[2], a).next();
        bb.vertex(mat, x + t, y1, z).color(c1[0], c1[1], c1[2], a).next();
        bb.vertex(mat, x + t, y2, z).color(c2[0], c2[1], c2[2], a).next();
        bb.vertex(mat, x - t, y2, z).color(c2[0], c2[1], c2[2], a).next();
        // Facing X
        bb.vertex(mat, x, y1, z - t).color(c1[0], c1[1], c1[2], a).next();
        bb.vertex(mat, x, y1, z + t).color(c1[0], c1[1], c1[2], a).next();
        bb.vertex(mat, x, y2, z + t).color(c2[0], c2[1], c2[2], a).next();
        bb.vertex(mat, x, y2, z - t).color(c2[0], c2[1], c2[2], a).next();
    }

    private static float[][] computePatternColors(String pattern, int base) {
        float r = ((base >> 16) & 0xFF) / 255.0f;
        float g = ((base >> 8)  & 0xFF) / 255.0f;
        float b = (base & 0xFF)          / 255.0f;

        long time = System.currentTimeMillis();
        String p = pattern != null ? pattern.toLowerCase() : "normal";

        switch (p) {
            case "rainbow" -> {
                float hue1 = (time % 3000L) / 3000.0f;
                float hue2 = (hue1 + 0.35f) % 1.0f;
                int c1 = Color.HSBtoRGB(hue1, 0.90f, 1.0f);
                int c2 = Color.HSBtoRGB(hue2, 0.90f, 1.0f);
                return new float[][]{
                        {((c1 >> 16) & 0xFF) / 255f, ((c1 >> 8) & 0xFF) / 255f, (c1 & 0xFF) / 255f},
                        {((c2 >> 16) & 0xFF) / 255f, ((c2 >> 8) & 0xFF) / 255f, (c2 & 0xFF) / 255f}
                };
            }
            case "cyber" -> {
                // Vibrant Cyberpunk: Neon Cyan (#00F0FF) to Electric Pink (#FF007F)
                float wave = 0.5f + 0.5f * (float) Math.sin(time / 320.0);
                float[] cyan = {0.0f, 0.94f, 1.0f};
                float[] magenta = {1.0f, 0.0f, 0.55f};
                return new float[][]{
                        {cyan[0] * wave + magenta[0] * (1f - wave), cyan[1] * wave + magenta[1] * (1f - wave), cyan[2] * wave + magenta[2] * (1f - wave)},
                        {magenta[0] * wave + cyan[0] * (1f - wave), magenta[1] * wave + cyan[1] * (1f - wave), magenta[2] * wave + cyan[2] * (1f - wave)}
                };
            }
            case "aurora" -> {
                // Northern lights: Emerald green (#00FFA3) into Celestial Turquoise (#00D4FF) into Violet (#7928CA)
                float t1 = 0.5f + 0.5f * (float) Math.sin(time / 450.0);
                float t2 = 0.5f + 0.5f * (float) Math.cos(time / 600.0);
                float[] top = {0.0f * (1f - t1) + 0.0f * t1, 1.0f * (1f - t1) + 0.83f * t1, 0.64f * (1f - t1) + 1.0f * t1};
                float[] bot = {0.47f * t2 + 0.0f * (1f - t2), 0.16f * t2 + 0.9f * (1f - t2), 0.79f * t2 + 1.0f * (1f - t2)};
                return new float[][]{top, bot};
            }
            case "fire" -> {
                // Blazing inferno: Solar Golden Yellow (#FFE600) on top to Magma Crimson (#FF1E00) on bottom
                float flicker = 0.82f + 0.18f * (float) Math.sin(time / 140.0 + Math.cos(time / 90.0));
                return new float[][]{
                        {1.0f, 0.90f * flicker, 0.0f},
                        {1.0f * flicker, 0.12f, 0.0f}
                };
            }
            case "plasma" -> {
                // High-energy plasma: Electric Violet (#8A2BE2) to Radiant Neon Pink (#FF1493)
                float f = 0.5f + 0.5f * (float) Math.sin(time / 240.0);
                return new float[][]{
                        {0.54f * f + 1.0f * (1f - f), 0.17f * f + 0.08f * (1f - f), 0.89f * f + 0.58f * (1f - f)},
                        {1.0f * f + 0.54f * (1f - f), 0.08f * f + 0.17f * (1f - f), 0.58f * f + 0.89f * (1f - f)}
                };
            }
            case "glitch" -> {
                // Cyberpunk glitch: stepped color hopping with chromatic aberration
                int phase = (int) ((time / 160L) % 4);
                return switch (phase) {
                    case 0 -> new float[][]{{0.0f, 1.0f, 1.0f}, {1.0f, 0.0f, 0.3f}};
                    case 1 -> new float[][]{{1.0f, 0.0f, 0.5f}, {0.1f, 1.0f, 0.8f}};
                    case 2 -> new float[][]{{1.0f, 1.0f, 0.0f}, {0.8f, 0.0f, 1.0f}};
                    default -> new float[][]{{0.0f, 0.8f, 1.0f}, {1.0f, 0.2f, 0.0f}};
                };
            }
            case "pulse" -> {
                float pulse = 0.55f + 0.45f * (float) Math.sin(time / 260.0);
                float[] col = {r * pulse, g * pulse, b * pulse};
                return new float[][]{col, col};
            }
            default -> {
                // Normal mode: clean customized base color with slight top specular highlight
                float[] top = {Math.min(1.0f, r + 0.15f), Math.min(1.0f, g + 0.15f), Math.min(1.0f, b + 0.15f)};
                float[] bot = {r, g, b};
                return new float[][]{top, bot};
            }
        }
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0x00D4FF;
        }
    }
}
