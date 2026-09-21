package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
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

public class BlurRenderer {

    // === Основной FBO (полный размер) ===
    private static int program = -1, vao = -1, vbo = -1;
    private static int fboA = -1, fboB = -1, texA = -1, texB = -1;
    private static int width = 0, height = 0;
    private static boolean failed = false;
    private static int uDiffuse = -1, uBlurDir = -1, uRadius = -1;

    // === Для блюра под HUD виджетами ===
    private static int capFboA = -1, capFboB = -1;
    private static int capTexA = -1, capTexB = -1;
    private static int capSmallW = 0, capSmallH = 0;
    private static int capFullW = 0, capFullH = 0;
    private static final int CAP_DOWNSAMPLE = 4;
    private static final int CAP_RADIUS = 12;

    // ============================================================
    // ПОЛНОЭКРАННЫЙ БЛЮР (для меню)
    // ============================================================
    public static void applyFullscreen(int radius) {
        if (failed || radius <= 0) return;
        if (!ConfigManager.INSTANCE.enableGlassBlur) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) return;

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        try {
            if (program == -1) init();
            if (fboA == -1 || w != width || h != height) resize(w, h);

            int mainFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

            int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] prevVp = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevVp);

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);

            // 1. main -> texA
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboA);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            // 2. Horizontal: texA -> fboB
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboB);
            GL11.glViewport(0, 0, w, h);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL20.glUseProgram(program);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
            GL20.glUniform1i(uDiffuse, 0);
            GL20.glUniform2f(uBlurDir, 1f, 0f);
            GL20.glUniform1f(uRadius, radius);
            drawQuad();

            // 3. Vertical: texB -> fboA
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboA);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);
            GL20.glUniform2f(uBlurDir, 0f, 1f);
            drawQuad();

            // 4. texA -> main
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fboA);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            // Restore
            GL20.glUseProgram(prevProgram);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFbo);
            GL30.glBindVertexArray(prevVao);
            GL13.glActiveTexture(prevActive);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL11.glViewport(prevVp[0], prevVp[1], prevVp[2], prevVp[3]);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);

            mc.getFramebuffer().beginWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);        
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        } catch (Throwable t) {
            failed = true;
            System.out.println("[MacClient] Blur FAILED: " + t.getMessage());
            t.printStackTrace();
        }
    }

    // ============================================================
    // CAPTURE ДЛЯ HUD (размывает весь экран в маленький FBO)
    // ============================================================
    public static void captureBlurredBackground() {
        if (failed) return;
        if (!ConfigManager.INSTANCE.enableGlassBlur) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) return;

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        try {
            if (program == -1) init();

            int sw = Math.max(1, w / CAP_DOWNSAMPLE);
            int sh = Math.max(1, h / CAP_DOWNSAMPLE);

            if (capSmallW != sw || capSmallH != sh || capFboA == -1) {
                if (capFboA != -1) {
                    GL30.glDeleteFramebuffers(capFboA);
                    GL30.glDeleteFramebuffers(capFboB);
                    GL11.glDeleteTextures(capTexA);
                    GL11.glDeleteTextures(capTexB);
                }
                capSmallW = sw;
                capSmallH = sh;
                capTexA = createTexture(sw, sh);
                capTexB = createTexture(sw, sh);
                capFboA = createFBO(capTexA);
                capFboB = createFBO(capTexB);
            }
            capFullW = w;
            capFullH = h;

            int mainFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

            int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            int[] prevVp = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevVp);

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);

            // Downsample main -> capFboA (с LINEAR — сглаживает при уменьшении)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capFboA);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, sw, sh,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

            // Horizontal: capTexA -> capFboB
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, capFboB);
            GL11.glViewport(0, 0, sw, sh);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL20.glUseProgram(program);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, capTexA);
            GL20.glUniform1i(uDiffuse, 0);
            GL20.glUniform2f(uBlurDir, 1f, 0f);
            GL20.glUniform1f(uRadius, CAP_RADIUS);
            drawQuad();

            // Vertical: capTexB -> capFboA (результат в capTexA)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, capFboA);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, capTexB);
            GL20.glUniform2f(uBlurDir, 0f, 1f);
            drawQuad();

            // Restore
            GL20.glUseProgram(prevProgram);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFbo);
            GL30.glBindVertexArray(prevVao);
            GL13.glActiveTexture(prevActive);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL11.glViewport(prevVp[0], prevVp[1], prevVp[2], prevVp[3]);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);

            mc.getFramebuffer().beginWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        } catch (Throwable t) {
            failed = true;
            System.out.println("[MacClient] Blur capture FAILED: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /** Рисует размытую текстуру над регионом виджета. x,y,w,h — в GUI-координатах. */
    public static void drawBlurredRegion(DrawContext ctx, int x, int y, int w, int h) {
        if (failed || capTexA == -1) return;
        if (w <= 0 || h <= 0) return;
        if (capFullW <= 0 || capFullH <= 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();

        double sx = (double) fbW / mc.getWindow().getScaledWidth();
        double sy = (double) fbH / mc.getWindow().getScaledHeight();

        int fx = (int)(x * sx);
        int fy = (int)(y * sy);
        int fw = (int)(w * sx);
        int fh = (int)(h * sy);

        // U — горизонталь, V — вертикаль
        float u0 = (float) fx / capFullW;
        float u1 = (float)(fx + fw) / capFullW;

        // V инвертирован: верх экрана = верх текстуры (v=1), низ = v=0
        float v_top = 1.0f - (float) fy / capFullH;
        float v_bot = 1.0f - (float)(fy + fh) / capFullH;

        // ВАЖНО: сначала шейдер, потом текстура
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, capTexA);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        // top-left, top-right, bottom-right, bottom-left
        buf.vertex(mat, x, y, 0).texture(u0, v_top).next();
        buf.vertex(mat, x + w, y, 0).texture(u1, v_top).next();
        buf.vertex(mat, x + w, y + h, 0).texture(u1, v_bot).next();
        buf.vertex(mat, x, y + h, 0).texture(u0, v_bot).next();
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        ctx.fill(x, y, x + w, y + h, 0x40FF0000);
        
        // === Восстанавливаем state для следующих рендеров ===
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================
    private static void init() throws Exception {
        int vs = compile(GL20.GL_VERTEX_SHADER, "blur.vsh");
        int fs = compile(GL20.GL_FRAGMENT_SHADER, "blur.fsh");
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Link: " + GL20.glGetProgramInfoLog(program));
        }
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        uDiffuse = GL20.glGetUniformLocation(program, "DiffuseSampler");
        uBlurDir = GL20.glGetUniformLocation(program, "BlurDir");
        uRadius  = GL20.glGetUniformLocation(program, "Radius");

        float[] verts = {
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
             1f,  1f, 1f, 1f,
            -1f, -1f, 0f, 0f,
             1f,  1f, 1f, 1f,
            -1f,  1f, 0f, 1f,
        };
        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, verts, GL30.GL_STATIC_DRAW);
        int stride = 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);
        GL30.glBindVertexArray(0);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);

        System.out.println("[MacClient] Blur initialized OK");
    }

    private static int compile(int type, String name) throws Exception {
        String path = "/assets/macclient/shaders/" + name;
        try (InputStream in = BlurRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Not found: " + path);
            String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int sh = GL20.glCreateShader(type);
            GL20.glShaderSource(sh, src);
            GL20.glCompileShader(sh);
            if (GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException(name + ": " + GL20.glGetShaderInfoLog(sh));
            }
            return sh;
        }
    }

    private static void drawQuad() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    private static void resize(int w, int h) {
        if (fboA != -1) {
            GL30.glDeleteFramebuffers(fboA);
            GL30.glDeleteFramebuffers(fboB);
            GL11.glDeleteTextures(texA);
            GL11.glDeleteTextures(texB);
        }
        width = w; height = h;
        texA = createTexture(w, h);
        texB = createTexture(w, h);
        fboA = createFBO(texA);
        fboB = createFBO(texB);
    }

    private static int createTexture(int w, int h) {
        int t = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return t;
    }

    private static int createFBO(int tex) {
        int f = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, f);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D, tex, 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.out.println("[MacClient] FBO incomplete!");
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return f;
    }
}