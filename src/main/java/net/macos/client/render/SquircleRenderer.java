package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.MacClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

/**
 * GPU-accelerated SDF Squircle / Rounded Rectangle Renderer.
 * Renders anti-aliased fills, vertical gradients, borders, and ambient drop shadows
 * with subpixel accuracy directly in fragment shader — eliminating all pixelation.
 */
public final class SquircleRenderer {

    private static boolean initialized = false;
    private static int program = -1;
    private static int vao = -1;
    private static int vbo = -1;

    // Uniform locations
    private static int uModelViewMat;
    private static int uProjMat;
    private static int uPos;
    private static int uSize;
    private static int uMargin;
    private static int uRadius;
    private static int uColor;
    private static int uColor2;
    private static int uMode;
    private static int uBorder;
    private static int uShadowBlur;

    private static final FloatBuffer matBuffer = BufferUtils.createFloatBuffer(16);

    private SquircleRenderer() {}

    public static void init() {
        if (initialized) return;
        try {
            String vshSrc = loadResource("/assets/macclient/shaders/squircle.vsh");
            String fshSrc = loadResource("/assets/macclient/shaders/squircle.fsh");

            int vs = compile(GL20.GL_VERTEX_SHADER, vshSrc, "squircle.vsh");
            int fs = compile(GL20.GL_FRAGMENT_SHADER, fshSrc, "squircle.fsh");

            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Squircle link failed: " + GL20.glGetProgramInfoLog(program));
            }

            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);

            // Locate uniforms
            uModelViewMat = GL20.glGetUniformLocation(program, "ModelViewMat");
            uProjMat      = GL20.glGetUniformLocation(program, "ProjMat");
            uPos          = GL20.glGetUniformLocation(program, "uPos");
            uSize         = GL20.glGetUniformLocation(program, "uSize");
            uMargin       = GL20.glGetUniformLocation(program, "uMargin");
            uRadius       = GL20.glGetUniformLocation(program, "uRadius");
            uColor        = GL20.glGetUniformLocation(program, "uColor");
            uColor2       = GL20.glGetUniformLocation(program, "uColor2");
            uMode         = GL20.glGetUniformLocation(program, "uMode");
            uBorder       = GL20.glGetUniformLocation(program, "uBorder");
            uShadowBlur   = GL20.glGetUniformLocation(program, "uShadowBlur");

            // Static unit quad: (0,0) to (1,1)
            float[] quad = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 0f,
                1f, 1f,
                0f, 1f
            };

            vao = GL30.glGenVertexArrays();
            vbo = GL30.glGenBuffers();
            GL30.glBindVertexArray(vao);
            GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL30.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);

            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0L);

            GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);

            initialized = true;
            MacClient.LOGGER.info("[MacClient][SquircleRenderer] Initialized successfully, program=" + program);
        } catch (Exception e) {
            MacClient.LOGGER.error("[MacClient][SquircleRenderer] Init failed!", e);
        }
    }

    // ─── Solid Fill ─────────────────────────────────────────────────────────

    public static void fill(DrawContext ctx, float x, float y, float w, float h, float radius, int color) {
        fill(ctx, x, y, w, h, radius, radius, radius, radius, color);
    }

    public static void fill(DrawContext ctx, float x, float y, float w, float h,
                            float tl, float tr, float br, float bl, int color) {
        fillGradient(ctx, x, y, w, h, tl, tr, br, bl, color, color);
    }

    // ─── Gradient Fill ──────────────────────────────────────────────────────

    public static void fillGradient(DrawContext ctx, float x, float y, float w, float h,
                                    float radius, int colorTop, int colorBot) {
        fillGradient(ctx, x, y, w, h, radius, radius, radius, radius, colorTop, colorBot);
    }

    public static void fillGradient(DrawContext ctx, float x, float y, float w, float h,
                                    float tl, float tr, float br, float bl,
                                    int colorTop, int colorBot) {
        if (w <= 0f || h <= 0f || ctx == null) return;
        ensureReady();
        if (!initialized) {
            fillFallback(ctx, (int) x, (int) y, (int) w, (int) h, (int) Math.max(tl, tr), colorTop);
            return;
        }

        try {
            setupRenderState(ctx);
            GL20.glUseProgram(program);

            setMatrices(ctx);
            GL20.glUniform2f(uPos, x, y);
            GL20.glUniform2f(uSize, w, h);
            GL20.glUniform1f(uMargin, 2.0f);
            GL20.glUniform4f(uRadius, tl, tr, br, bl);
            setColor(uColor, colorTop);
            setColor(uColor2, colorBot);
            GL20.glUniform1i(uMode, 0);
            GL20.glUniform1f(uBorder, 0f);
            GL20.glUniform1f(uShadowBlur, 0f);

            drawQuad();
        } catch (Throwable t) {
            MacClient.LOGGER.error("[MacClient][SquircleRenderer] fill failed", t);
            fillFallback(ctx, (int) x, (int) y, (int) w, (int) h, (int) Math.max(tl, tr), colorTop);
        } finally {
            cleanupRenderState();
        }
    }

    // ─── Border Outline ─────────────────────────────────────────────────────

    public static void border(DrawContext ctx, float x, float y, float w, float h,
                              float radius, float thickness, int color) {
        borderGradient(ctx, x, y, w, h, radius, thickness, color, color);
    }

    public static void borderGradient(DrawContext ctx, float x, float y, float w, float h,
                                      float radius, float thickness,
                                      int colorTop, int colorBot) {
        if (w <= 0f || h <= 0f || thickness <= 0f || ctx == null) return;
        ensureReady();
        if (!initialized) {
            borderFallback(ctx, (int) x, (int) y, (int) w, (int) h, (int) radius, (int) Math.max(1, thickness), colorTop);
            return;
        }

        try {
            setupRenderState(ctx);
            GL20.glUseProgram(program);

            setMatrices(ctx);
            GL20.glUniform2f(uPos, x, y);
            GL20.glUniform2f(uSize, w, h);
            GL20.glUniform1f(uMargin, thickness + 2.0f);
            GL20.glUniform4f(uRadius, radius, radius, radius, radius);
            setColor(uColor, colorTop);
            setColor(uColor2, colorBot);
            GL20.glUniform1i(uMode, 1);
            GL20.glUniform1f(uBorder, thickness);
            GL20.glUniform1f(uShadowBlur, 0f);

            drawQuad();
        } catch (Throwable t) {
            MacClient.LOGGER.error("[MacClient][SquircleRenderer] border failed", t);
            borderFallback(ctx, (int) x, (int) y, (int) w, (int) h, (int) radius, (int) Math.max(1, thickness), colorTop);
        } finally {
            cleanupRenderState();
        }
    }

    // ─── Ambient Shadow / Neon Glow ────────────────────────────────────────

    public static void shadow(DrawContext ctx, float x, float y, float w, float h,
                              float radius, float blurRadius, int color) {
        if (w <= 0f || h <= 0f || blurRadius <= 0.1f || ctx == null) return;
        ensureReady();
        if (!initialized) return;

        try {
            setupRenderState(ctx);
            GL20.glUseProgram(program);

            setMatrices(ctx);
            GL20.glUniform2f(uPos, x, y);
            GL20.glUniform2f(uSize, w, h);
            GL20.glUniform1f(uMargin, blurRadius + 2.0f);
            GL20.glUniform4f(uRadius, radius, radius, radius, radius);
            setColor(uColor, color);
            setColor(uColor2, color);
            GL20.glUniform1i(uMode, 2);
            GL20.glUniform1f(uBorder, 0f);
            GL20.glUniform1f(uShadowBlur, blurRadius);

            drawQuad();
        } catch (Throwable ignored) {
        } finally {
            cleanupRenderState();
        }
    }

    // ─── High-Level Helpers ─────────────────────────────────────────────────

    public static void card(DrawContext ctx, float x, float y, float w, float h,
                            float radius, int bgColor, float borderThickness, int borderColor) {
        if (w <= 0f || h <= 0f) return;
        fill(ctx, x, y, w, h, radius, bgColor);
        if (borderThickness > 0.001f) {
            border(ctx, x, y, w, h, radius, borderThickness, borderColor);
        }
    }

    public static void pill(DrawContext ctx, float x, float y, float w, float h, int color) {
        fill(ctx, x, y, w, h, h / 2.0f, color);
    }

    public static void circle(DrawContext ctx, float cx, float cy, float r, int color) {
        fill(ctx, cx - r, cy - r, r * 2.0f, r * 2.0f, r, color);
    }

    // ─── Fallbacks ──────────────────────────────────────────────────────────

    public static void fillFallback(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        int a = (color >>> 24) & 0xFF;
        if (a <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i - 1;
            int span = (int) Math.sqrt(r * r - dy * dy);
            ctx.fill(x + r - span, y + i, x + r, y + i + 1, color);
            ctx.fill(x + w - r, y + i, x + w - r + span, y + i + 1, color);
            ctx.fill(x + r - span, y + h - 1 - i, x + r, y + h - i, color);
            ctx.fill(x + w - r, y + h - 1 - i, x + w - r + span, y + h - i, color);
        }
    }

    public static void borderFallback(DrawContext ctx, int x, int y, int w, int h, int r, int thickness, int color) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        int a = (color >>> 24) & 0xFF;
        if (a <= 0) return;
        ctx.fill(x, y, x + w, y + thickness, color);
        ctx.fill(x, y + h - thickness, x + w, y + h, color);
        ctx.fill(x, y + thickness, x + thickness, y + h - thickness, color);
        ctx.fill(x + w - thickness, y + thickness, x + w, y + h - thickness, color);
    }

    // ─── Private GL State / Setup ───────────────────────────────────────────

    private static void ensureReady() {
        if (!initialized) init();
    }

    private static void setupRenderState(DrawContext ctx) {
        if (ctx != null) {
            ctx.draw();
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
    }

    private static void cleanupRenderState() {
        GL20.glUseProgram(0);
        GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    private static void setMatrices(DrawContext ctx) {
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(ctx.getMatrices().peek().getPositionMatrix());
        Matrix4f pr = RenderSystem.getProjectionMatrix();

        matBuffer.clear();
        mv.get(matBuffer);
        matBuffer.rewind();
        GL20.glUniformMatrix4fv(uModelViewMat, false, matBuffer);

        matBuffer.clear();
        pr.get(matBuffer);
        matBuffer.rewind();
        GL20.glUniformMatrix4fv(uProjMat, false, matBuffer);
    }

    private static void setColor(int loc, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8)  & 0xFF) / 255.0f;
        float b = (argb & 0xFF)          / 255.0f;
        GL20.glUniform4f(loc, r, g, b, a);
    }

    private static void drawQuad() {
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0L);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private static int compile(int type, String src, String name) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Compile error in " + name + ": " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private static String loadResource(String path) throws Exception {
        try (InputStream in = SquircleRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
