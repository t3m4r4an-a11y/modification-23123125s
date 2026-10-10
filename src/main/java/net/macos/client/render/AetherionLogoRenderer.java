package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * High-definition renderer for the official Aetherion Project 2026 emblem.
 * Features:
 * - Dynamic color tinting (solid hex, accent sync, or real-time chromatic aurora gradient).
 * - Matches the menu aurora shader colors for 100% aesthetic harmony.
 */
public final class AetherionLogoRenderer {

    public static final Identifier LOGO = new Identifier("macclient", "textures/gui/aetherion_logo.png");

    private AetherionLogoRenderer() {}

    /**
     * Draw emblem with a solid or accent color.
     */
    public static void draw(DrawContext ctx, int x, int y, int size, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8)  & 0xFF) / 255.0f;
        float b = (argb & 0xFF)         / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);
        ctx.drawTexture(LOGO, x, y, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /**
     * Draw emblem with a live chromatic aurora gradient (shifting Cyan to Electric Violet).
     */
    public static void drawChromatic(DrawContext ctx, int x, int y, int size, float alpha) {
        long time = System.currentTimeMillis();
        float t = (float) (time % 4000L) / 4000.0f;
        float wave = 0.5f + 0.5f * (float) Math.sin(time / 450.0);

        // Top color: Shifting between Neon Cyan (#00F0FF) and Arctic Emerald (#00FFA3)
        float r1 = 0.0f;
        float g1 = 0.94f + 0.06f * wave;
        float b1 = 1.0f - 0.35f * wave;

        // Bottom color: Shifting between Celestial Violet (#7928CA) and Hot Magenta (#FF007F)
        float r2 = 0.55f + 0.45f * wave;
        float g2 = 0.10f;
        float b2 = 0.90f - 0.35f * wave;

        float a = Math.max(0f, Math.min(1f, alpha));

        ctx.draw();
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, LOGO);

        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Top-left
        bb.vertex(mat, x, y, 0).texture(0f, 0f).color(r1, g1, b1, a).next();
        // Bottom-left
        bb.vertex(mat, x, y + size, 0).texture(0f, 1f).color(r2, g2, b2, a).next();
        // Bottom-right
        bb.vertex(mat, x + size, y + size, 0).texture(1f, 1f).color(r2, g2, b2, a).next();
        // Top-right
        bb.vertex(mat, x + size, y, 0).texture(1f, 0f).color(r1, g1, b1, a).next();

        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShader(() -> null);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
