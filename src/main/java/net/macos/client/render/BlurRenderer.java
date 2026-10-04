package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.MacClient;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Fullscreen destructive blur — used only for the menu-open backdrop. */
public final class BlurRenderer {

    private BlurRenderer() {}

    private static final int MAX_RADIUS = 32;

    private static int program = -1;
    private static int vao = -1;
    private static int vbo = -1;
    private static int uDiffuse = -1;
    private static int uBlurDir = -1;
    private static int uRadius = -1;

    private static int fboA = -1, fboB = -1;
    private static int texA = -1, texB = -1;
    private static int width = 0, height = 0;

    private static boolean initialized = false;

    public static void applyFullscreen(int radius) {
        if (!ConfigManager.INSTANCE.enableGlassBlur) return;
        radius = clampRadius(radius);
        if (radius <= 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) return;

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        try {
            ensureInitialized();
            if (fboA == -1 || fboB == -1 || width != w || height != h) {
                resize(w, h);
            }

            int mainFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            GLState state = GLState.capture();

            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_CULL_FACE);

                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboA);
                GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboB);
                GL11.glViewport(0, 0, w, h);
                GL11.glClearColor(0f, 0f, 0f, 0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                GL20.glUseProgram(program);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
                GL20.glUniform1i(uDiffuse, 0);
                GL20.glUniform2f(uBlurDir, 1f, 0f);
                GL20.glUniform1f(uRadius, radius);
                drawQuad();

                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboA);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);
                GL20.glUniform2f(uBlurDir, 0f, 1f);
                drawQuad();

                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fboA);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);
                GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            } finally {
                state.restore();
                mc.getFramebuffer().beginWrite(false);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        } catch (Throwable t) {
            MacClient.LOGGER.error("Blur fullscreen failed", t);
        }
    }

    private static void ensureInitialized() throws Exception {
        if (initialized && program != -1 && vao != -1 && vbo != -1) return;
        init();
        initialized = true;
    }

    private static void init() throws Exception {
        int vertexShader = compile(GL20.GL_VERTEX_SHADER, "blur.vsh");
        int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, "blur.fsh");
        try {
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertexShader);
            GL20.glAttachShader(program, fragmentShader);
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glBindAttribLocation(program, 1, "UV0");
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Blur shader link failed: " + GL20.glGetProgramInfoLog(program));
            }
            uDiffuse = GL20.glGetUniformLocation(program, "DiffuseSampler");
            uBlurDir = GL20.glGetUniformLocation(program, "BlurDir");
            uRadius = GL20.glGetUniformLocation(program, "Radius");

            float[] vertices = {
                    -1f, -1f, 0f, 0f,  1f, -1f, 1f, 0f,  1f, 1f, 1f, 1f,
                    -1f, -1f, 0f, 0f,  1f, 1f, 1f, 1f,  -1f, 1f, 0f, 1f
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
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);

            MacClient.LOGGER.info("BlurRenderer initialized");
        } finally {
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
        }
    }

    private static int compile(int type, String name) throws Exception {
        String path = "/assets/macclient/shaders/" + name;
        try (InputStream input = BlurRenderer.class.getResourceAsStream(path)) {
            if (input == null) throw new RuntimeException("Shader not found: " + path);
            String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new RuntimeException(name + " compilation failed:\n" + log);
            }
            return shader;
        }
    }

    private static void drawQuad() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    private static void resize(int w, int h) {
        deleteBuffers();
        width = w;
        height = h;
        texA = createTexture(w, h);
        texB = createTexture(w, h);
        fboA = createFBO(texA);
        fboB = createFBO(texB);
    }

    private static int createTexture(int w, int h) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static int createFBO(int texture) {
        int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            MacClient.LOGGER.error("Blur FBO incomplete: 0x{}", Integer.toHexString(status));
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return fbo;
    }

    private static void deleteBuffers() {
        if (fboA != -1) { GL30.glDeleteFramebuffers(fboA); fboA = -1; }
        if (fboB != -1) { GL30.glDeleteFramebuffers(fboB); fboB = -1; }
        if (texA != -1) { GL11.glDeleteTextures(texA); texA = -1; }
        if (texB != -1) { GL11.glDeleteTextures(texB); texB = -1; }
    }

    private static int clampRadius(int radius) {
        return Math.max(0, Math.min(MAX_RADIUS, radius));
    }

    private static final class GLState {
        private final int framebuffer, readFramebuffer, drawFramebuffer, program, vao, activeTexture, texture2D;
        private final int[] viewport = new int[4];
        private final boolean depthTest, blend, cullFace, depthMask;

        private GLState() {
            framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            texture2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        }

        private static GLState capture() { return new GLState(); }

        private void restore() {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);
            GL13.glActiveTexture(activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2D);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            set(GL11.GL_DEPTH_TEST, depthTest);
            set(GL11.GL_BLEND, blend);
            set(GL11.GL_CULL_FACE, cullFace);
            GL11.glDepthMask(depthMask);
        }

        private static void set(int cap, boolean enabled) {
            if (enabled) GL11.glEnable(cap); else GL11.glDisable(cap);
        }
    }
}