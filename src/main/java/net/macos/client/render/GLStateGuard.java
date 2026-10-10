package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Comprehensive OpenGL state snapshot + restore guard.
 *
 * Usage — wrap ANY raw GL2/GL3 shader pass:
 *
 *   try (var g = GLStateGuard.push()) {
 *       // raw GL calls here — all state is restored on exit
 *   }
 *   // Optionally re-enter Minecraft's main FBO:
 *   GLStateGuard.returnToMainFbo();
 *
 * This fixes the "no background / no text" widget glitches caused by:
 *  - colorMask being left as (true,true,true,false) after HandGlowRenderer crashes
 *  - Active custom VAO being left bound after SquircleRenderer
 *  - Wrong program being active when Minecraft tries to render text/quads
 *  - Viewport not restored after Kawase blur mip-chain
 *  - Blend func not restored → broken transparency on HUD elements
 */
public final class GLStateGuard implements AutoCloseable {

    // ─── Saved state ────────────────────────────────────────────────────────
    private final int savedFbo;
    private final int savedReadFbo;
    private final int savedDrawFbo;
    private final int savedProgram;
    private final int savedVao;
    private final int savedActiveTexture;
    private final int savedTex0;
    private final int savedTex1;
    private final int savedTex2;
    private final int[] savedViewport = new int[4];
    private final boolean savedDepthTest;
    private final boolean savedDepthMask;
    private final boolean savedBlend;
    private final boolean savedCullFace;
    private final boolean savedColorMaskR;
    private final boolean savedColorMaskG;
    private final boolean savedColorMaskB;
    private final boolean savedColorMaskA;
    private final int savedSrcBlendRgb;
    private final int savedDstBlendRgb;
    private final int savedSrcBlendAlpha;
    private final int savedDstBlendAlpha;

    private GLStateGuard() {
        savedFbo          = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        savedReadFbo      = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        savedDrawFbo      = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        savedProgram      = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        savedVao          = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        savedActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        // Save binding for tex slots 0-2 (those we commonly stomp)
        GL13.glActiveTexture(GL13.GL_TEXTURE0); savedTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1); savedTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE2); savedTex2 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(savedActiveTexture);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
        savedDepthTest  = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedDepthMask  = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedBlend      = GL11.glIsEnabled(GL11.GL_BLEND);
        savedCullFace   = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        // colorMask — OpenGL returns 4 booleans packed
        java.nio.ByteBuffer cm = org.lwjgl.BufferUtils.createByteBuffer(4);
        org.lwjgl.opengl.GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, cm);
        savedColorMaskR = cm.get(0) != 0;
        savedColorMaskG = cm.get(1) != 0;
        savedColorMaskB = cm.get(2) != 0;
        savedColorMaskA = cm.get(3) != 0;
        savedSrcBlendRgb   = GL11.glGetInteger(GL20.GL_BLEND_SRC_RGB);
        savedDstBlendRgb   = GL11.glGetInteger(GL20.GL_BLEND_DST_RGB);
        savedSrcBlendAlpha = GL11.glGetInteger(GL20.GL_BLEND_SRC_ALPHA);
        savedDstBlendAlpha = GL11.glGetInteger(GL20.GL_BLEND_DST_ALPHA);
    }

    /** Capture current GL state and return a guard object. Use in try-with-resources. */
    public static GLStateGuard push() {
        return new GLStateGuard();
    }

    /** Restore everything exactly as it was before push(). */
    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, savedReadFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, savedDrawFbo);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo);
        GL20.glUseProgram(savedProgram);
        GL30.glBindVertexArray(savedVao);

        GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedTex0);
        GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedTex1);
        GL13.glActiveTexture(GL13.GL_TEXTURE2); GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedTex2);
        GL13.glActiveTexture(savedActiveTexture);

        RenderSystem.setShaderTexture(0, savedTex0);
        RenderSystem.setShaderTexture(1, savedTex1);
        RenderSystem.setShaderTexture(2, savedTex2);
        RenderSystem.setShader(() -> null);

        GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        setEnabled(GL11.GL_DEPTH_TEST, savedDepthTest);
        GL11.glDepthMask(savedDepthMask);
        setEnabled(GL11.GL_BLEND, savedBlend);
        setEnabled(GL11.GL_CULL_FACE, savedCullFace);
        GL11.glColorMask(savedColorMaskR, savedColorMaskG, savedColorMaskB, savedColorMaskA);
        if (savedBlend) {
            org.lwjgl.opengl.GL14.glBlendFuncSeparate(savedSrcBlendRgb, savedDstBlendRgb, savedSrcBlendAlpha, savedDstBlendAlpha);
        }

        // Always reset RenderSystem high-level state too
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** After a raw-GL pass, re-bind Minecraft's main FBO so subsequent DrawContext calls work. */
    public static void returnToMainFbo() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getFramebuffer() != null) {
            mc.getFramebuffer().beginWrite(false);
            int w = mc.getWindow().getFramebufferWidth();
            int h = mc.getWindow().getFramebufferHeight();
            RenderSystem.viewport(0, 0, w, h);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void setEnabled(int cap, boolean en) {
        if (en) GL11.glEnable(cap); else GL11.glDisable(cap);
    }
}
