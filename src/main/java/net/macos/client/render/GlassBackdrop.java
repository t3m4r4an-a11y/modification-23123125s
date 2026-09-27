package net.macos.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Liquid Glass backdrop: once per frame capture world → blur → sample under panels.
 * Foundation for all glass HUD / menu surfaces.
 */
public final class GlassBackdrop {

    private GlassBackdrop() {}

    private static final int DOWNSAMPLE = 2;

    private static int program = -1;
    private static int uDiffuse = -1;
    private static int uBlurDir = -1;
    private static int uRadius = -1;
    private static int vao = -1;
    private static int vbo = -1;

    private static Framebuffer ping;
    private static Framebuffer pong;

    private static int fbW;
    private static int fbH;
    private static boolean ready;
    private static boolean capturedThisFrame;
    private static long lastFrame = -1;

    public static void beginFrame() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        long f = mc.world != null ? mc.world.getTime() : System.nanoTime();
        // reset once per client tick-ish; also allow multiple captures same tick after resize
        if (f != lastFrame) {
            lastFrame = f;
            capturedThisFrame = false;
        }
    }

    /**
     * Capture + blur the current main framebuffer into the glass backdrop texture.
     * Safe to call multiple times; only the first call per frame does work.
     */
    public static void capture() {
        if (!ConfigManager.INSTANCE.enableGlassBlur) {
            ready = false;
            return;
        }
        if (capturedThisFrame && ready) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) {
            ready = false;
            return;
        }

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) {
            ready = false;
            return;
        }

        try {
            ensureShader();
            ensureTargets(w, h);

            int radius = Math.max(1, Math.min(32, ConfigManager.INSTANCE.blurRadius));
            int sw = Math.max(1, w / DOWNSAMPLE);
            int sh = Math.max(1, h / DOWNSAMPLE);

            Framebuffer main = mc.getFramebuffer();
            int mainId = main.fbo;

            // 1) main → ping (downsample, linear = pre-blur)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ping.fbo);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, sw, sh,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

            // 2) H blur ping → pong
            pong.beginWrite(true);
            GL11.glViewport(0, 0, sw, sh);
            runBlurPass(ping.getColorAttachment(), 1f, 0f, radius);

            // 3) V blur pong → ping
            ping.beginWrite(true);
            GL11.glViewport(0, 0, sw, sh);
            runBlurPass(pong.getColorAttachment(), 0f, 1f, radius);

            main.beginWrite(false);
            RenderSystem.viewport(0, 0, w, h);

            ready = true;
            capturedThisFrame = true;
        } catch (Throwable t) {
            ready = false;
            System.err.println("[MacClient] GlassBackdrop.capture failed:");
            t.printStackTrace();
            try {
                MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Draw blurred backdrop in GUI coordinates (top-left origin).
     */
    public static void draw(DrawContext ctx, int x, int y, int w, int h) {
        if (!ready || ping == null || w <= 0 || h <= 0) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }

        int guiW = mc.getWindow().getScaledWidth();
        int guiH = mc.getWindow().getScaledHeight();
        if (guiW <= 0 || guiH <= 0) {
            return;
        }

        // UVs in top-left GUI space; MC FB texture is flipped on V
        float u0 = (float) x / (float) guiW;
        float u1 = (float) (x + w) / (float) guiW;
        float vTop = (float) y / (float) guiH;
        float vBot = (float) (y + h) / (float) guiH;
        float v0 = 1.0f - vBot;
        float v1 = 1.0f - vTop;

        int tex = ping.getColorAttachment();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, tex);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x, y, 0).texture(u0, v1).next();
        buffer.vertex(matrix, x + w, y, 0).texture(u1, v1).next();
        buffer.vertex(matrix, x + w, y + h, 0).texture(u1, v0).next();
        buffer.vertex(matrix, x, y + h, 0).texture(u0, v0).next();
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static boolean isReady() {
        return ready;
    }

    private static void runBlurPass(int texture, float dirX, float dirY, float radius) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL20.glUseProgram(program);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL20.glUniform1i(uDiffuse, 0);
        GL20.glUniform2f(uBlurDir, dirX, dirY);
        GL20.glUniform1f(uRadius, radius);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    private static void ensureTargets(int w, int h) {
        int sw = Math.max(1, w / DOWNSAMPLE);
        int sh = Math.max(1, h / DOWNSAMPLE);
        if (ping == null || fbW != w || fbH != h) {
            deleteTargets();
            fbW = w;
            fbH = h;
            // SimpleFramebuffer(width, height, useDepth, getError)
            ping = new SimpleFramebuffer(sw, sh, false, MinecraftClient.IS_SYSTEM_MAC);
            pong = new SimpleFramebuffer(sw, sh, false, MinecraftClient.IS_SYSTEM_MAC);
            ping.setClearColor(0, 0, 0, 0);
            pong.setClearColor(0, 0, 0, 0);
        }
    }

    private static void deleteTargets() {
        if (ping != null) {
            ping.delete();
            ping = null;
        }
        if (pong != null) {
            pong.delete();
            pong = null;
        }
    }

    private static void ensureShader() throws Exception {
        if (program != -1) {
            return;
        }

        int vs = compile(GL20.GL_VERTEX_SHADER, "blur.vsh");
        int fs = compile(GL20.GL_FRAGMENT_SHADER, "blur.fsh");
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "UV0");
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("GlassBackdrop link: " + GL20.glGetProgramInfoLog(program));
        }
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        uDiffuse = GL20.glGetUniformLocation(program, "DiffuseSampler");
        uBlurDir = GL20.glGetUniformLocation(program, "BlurDir");
        uRadius = GL20.glGetUniformLocation(program, "Radius");

        float[] vertices = {
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                1f, 1f, 1f, 1f,
                -1f, -1f, 0f, 0f,
                1f, 1f, 1f, 1f,
                -1f, 1f, 0f, 1f
        };
        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertices, GL30.GL_STATIC_DRAW);
        int stride = 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);
        GL30.glBindVertexArray(0);

        System.out.println("[MacClient] GlassBackdrop ready");
    }

    private static int compile(int type, String name) throws Exception {
        String path = "/assets/macclient/shaders/" + name;
        try (InputStream in = GlassBackdrop.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Missing shader " + path);
            }
            String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int sh = GL20.glCreateShader(type);
            GL20.glShaderSource(sh, src);
            GL20.glCompileShader(sh);
            if (GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(sh);
                GL20.glDeleteShader(sh);
                throw new RuntimeException(name + ":\n" + log);
            }
            return sh;
        }
    }
}
