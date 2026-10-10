package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * High-performance cinematic Motion Blur via accumulation framebuffer.
 */
public final class MotionBlurRenderer {

    private MotionBlurRenderer() {}

    private static Framebuffer accumFbo;
    private static int prevW = -1;
    private static int prevH = -1;

    public static void apply() {
        if (!ConfigManager.INSTANCE.enableMotionBlur) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) return;

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        if (accumFbo == null || prevW != w || prevH != h) {
            if (accumFbo != null) accumFbo.delete();
            accumFbo = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
            accumFbo.setClearColor(0f, 0f, 0f, 0f);
            accumFbo.clear(MinecraftClient.IS_SYSTEM_MAC);
            prevW = w;
            prevH = h;
        }

        try (var guard = GLStateGuard.push()) {
            float amount = Math.max(0.05f, Math.min(0.95f, ConfigManager.INSTANCE.motionBlurAmount));

            // Render previous accumulation texture over the current frame
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1f, 1f, 1f, amount);
            RenderSystem.setShaderTexture(0, accumFbo.getColorAttachment());

            Matrix4f matrix = new Matrix4f().identity();
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(matrix, -1f, -1f, 0f).texture(0f, 0f).next();
            buffer.vertex(matrix, 1f, -1f, 0f).texture(1f, 0f).next();
            buffer.vertex(matrix, 1f, 1f, 0f).texture(1f, 1f).next();
            buffer.vertex(matrix, -1f, 1f, 0f).texture(0f, 1f).next();
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            // Update accumulation FBO with current main framebuffer
            accumFbo.beginWrite(true);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(matrix, -1f, -1f, 0f).texture(0f, 0f).next();
            buffer.vertex(matrix, 1f, -1f, 0f).texture(1f, 0f).next();
            buffer.vertex(matrix, 1f, 1f, 0f).texture(1f, 1f).next();
            buffer.vertex(matrix, -1f, 1f, 0f).texture(0f, 1f).next();
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            mc.getFramebuffer().beginWrite(false);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}
