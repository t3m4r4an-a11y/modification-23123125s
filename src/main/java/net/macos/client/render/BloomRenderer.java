package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Bloom only for the FX layer (particles / target indicator / kill effects).
 *
 * Usage:
 *   BloomRenderer.beginFxLayer();
 *   // render TargetIndicator, HitFX, KillEffect
 *   BloomRenderer.endFxLayerAndApply();
 *
 * World and regular HUD are never bloomed.
 */
public final class BloomRenderer {

    private BloomRenderer() {}

    private static final int DOWNSAMPLE = 2;
    private static final int BLUR_RADIUS = 12;

    private static int extractProgram = -1;
    private static int blurProgram = -1;
    private static int blitProgram = -1;
    private static int addProgram = -1;

    private static int uExtractDiffuse = -1;
    private static int uExtractThreshold = -1;
    private static int uBlurDiffuse = -1;
    private static int uBlurDir = -1;
    private static int uBlurRadius = -1;
    private static int uBlitDiffuse = -1;
    private static int uAddDiffuse = -1;
    private static int uAddIntensity = -1;

    private static int vao = -1;
    private static int vbo = -1;

    private static int fxFbo = -1;
    private static int fxTex = -1;

    private static int fboA = -1;
    private static int fboB = -1;
    private static int texA = -1;
    private static int texB = -1;

    private static int smallW = 0;
    private static int smallH = 0;
    private static int fullW = 0;
    private static int fullH = 0;

    private static boolean initialized = false;
    private static boolean layerActive = false;

    public static void beginFxLayer() {
        if (!ConfigManager.INSTANCE.enableBloom) {
            layerActive = false;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) {
            layerActive = false;
            return;
        }

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) {
            layerActive = false;
            return;
        }

        try {
            ensureInitialized();
            int sw = Math.max(1, w / DOWNSAMPLE);
            int sh = Math.max(1, h / DOWNSAMPLE);
            if (fxFbo == -1 || fullW != w || fullH != h || smallW != sw || smallH != sh) {
                resize(sw, sh, w, h);
            }

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fxFbo);
            GL11.glViewport(0, 0, w, h);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            layerActive = true;
        } catch (Throwable t) {
            layerActive = false;
            System.err.println("[MacClient] Bloom beginFxLayer failed:");
            t.printStackTrace();
            if (mc.getFramebuffer() != null) {
                mc.getFramebuffer().beginWrite(false);
            }
        }
    }

    public static void endFxLayerAndApply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getFramebuffer() == null) {
            layerActive = false;
            return;
        }

        if (!layerActive) {
            mc.getFramebuffer().beginWrite(false);
            return;
        }

        float threshold = ConfigManager.INSTANCE.bloomThreshold;
        float intensity = ConfigManager.INSTANCE.bloomIntensity;
        int w = fullW;
        int h = fullH;
        int sw = smallW;
        int sh = smallH;

        try {
            GLState state = GLState.capture();
            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);

                // 1) Draw FX layer onto main (alpha blend)
                mc.getFramebuffer().beginWrite(false);
                GL11.glViewport(0, 0, w, h);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                GL20.glUseProgram(blitProgram);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, fxTex);
                GL20.glUniform1i(uBlitDiffuse, 0);
                drawQuad();

                if (intensity > 0.001f) {
                    GL11.glDisable(GL11.GL_BLEND);

                    // 2) Bright extract from FX only
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboA);
                    GL11.glViewport(0, 0, sw, sh);
                    GL11.glClearColor(0f, 0f, 0f, 0f);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                    GL20.glUseProgram(extractProgram);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, fxTex);
                    GL20.glUniform1i(uExtractDiffuse, 0);
                    GL20.glUniform1f(uExtractThreshold, threshold);
                    drawQuad();

                    // 3) H blur
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboB);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                    GL20.glUseProgram(blurProgram);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
                    GL20.glUniform1i(uBlurDiffuse, 0);
                    GL20.glUniform2f(uBlurDir, 1f, 0f);
                    GL20.glUniform1f(uBlurRadius, BLUR_RADIUS);
                    drawQuad();

                    // 4) V blur
                    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboA);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);
                    GL20.glUniform2f(uBlurDir, 0f, 1f);
                    drawQuad();

                    // 5) Additive bloom onto main (FX bloom only)
                    mc.getFramebuffer().beginWrite(false);
                    GL11.glViewport(0, 0, w, h);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);

                    GL20.glUseProgram(addProgram);
                    GL13.glActiveTexture(GL13.GL_TEXTURE0);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
                    GL20.glUniform1i(uAddDiffuse, 0);
                    GL20.glUniform1f(uAddIntensity, intensity);
                    drawQuad();
                }
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
            System.err.println("[MacClient] Bloom endFxLayer failed:");
            t.printStackTrace();
            mc.getFramebuffer().beginWrite(false);
        } finally {
            layerActive = false;
        }
    }

    private static void ensureInitialized() throws Exception {
        if (initialized && extractProgram != -1 && blurProgram != -1 && blitProgram != -1 && addProgram != -1) {
            return;
        }
        init();
        initialized = true;
    }

    private static void init() throws Exception {
        int vsExtract = -1, fsExtract = -1;
        int vsBlur = -1, fsBlur = -1;
        int vsBlit = -1, fsBlit = -1;
        int vsAdd = -1, fsAdd = -1;

        try {
            vsExtract = compile(GL20.GL_VERTEX_SHADER, "bloom_extract.vsh");
            fsExtract = compile(GL20.GL_FRAGMENT_SHADER, "bloom_extract.fsh");
            extractProgram = link(vsExtract, fsExtract, "bloom_extract");
            uExtractDiffuse = requireUniform(extractProgram, "DiffuseSampler");
            uExtractThreshold = requireUniform(extractProgram, "Threshold");

            vsBlur = compile(GL20.GL_VERTEX_SHADER, "blur.vsh");
            fsBlur = compile(GL20.GL_FRAGMENT_SHADER, "blur.fsh");
            blurProgram = link(vsBlur, fsBlur, "bloom_blur");
            uBlurDiffuse = requireUniform(blurProgram, "DiffuseSampler");
            uBlurDir = requireUniform(blurProgram, "BlurDir");
            uBlurRadius = requireUniform(blurProgram, "Radius");

            vsBlit = compile(GL20.GL_VERTEX_SHADER, "bloom_extract.vsh");
            fsBlit = compileSource(GL20.GL_FRAGMENT_SHADER,
                    "#version 150\n" +
                            "uniform sampler2D DiffuseSampler;\n" +
                            "in vec2 texCoord;\n" +
                            "out vec4 fragColor;\n" +
                            "void main() { fragColor = texture(DiffuseSampler, texCoord); }\n");
            blitProgram = link(vsBlit, fsBlit, "bloom_blit");
            uBlitDiffuse = requireUniform(blitProgram, "DiffuseSampler");

            vsAdd = compile(GL20.GL_VERTEX_SHADER, "bloom_extract.vsh");
            fsAdd = compileSource(GL20.GL_FRAGMENT_SHADER,
                    "#version 150\n" +
                            "uniform sampler2D DiffuseSampler;\n" +
                            "uniform float Intensity;\n" +
                            "in vec2 texCoord;\n" +
                            "out vec4 fragColor;\n" +
                            "void main() {\n" +
                            "    vec4 c = texture(DiffuseSampler, texCoord);\n" +
                            "    fragColor = vec4(c.rgb * Intensity, 1.0);\n" +
                            "}\n");
            addProgram = link(vsAdd, fsAdd, "bloom_add");
            uAddDiffuse = requireUniform(addProgram, "DiffuseSampler");
            uAddIntensity = requireUniform(addProgram, "Intensity");

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
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);

            System.out.println("[MacClient] BloomRenderer initialized (FX layer only)");
        } finally {
            deleteShader(vsExtract);
            deleteShader(fsExtract);
            deleteShader(vsBlur);
            deleteShader(fsBlur);
            deleteShader(vsBlit);
            deleteShader(fsBlit);
            deleteShader(vsAdd);
            deleteShader(fsAdd);
        }
    }

    private static int compileSource(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("inline shader compilation failed:\n" + log);
        }
        return shader;
    }

    private static int link(int vs, int fs, String name) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "UV0");
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(name + " link failed: " + GL20.glGetProgramInfoLog(program));
        }
        return program;
    }

    private static int requireUniform(int program, String name) {
        int loc = GL20.glGetUniformLocation(program, name);
        if (loc < 0) {
            throw new RuntimeException("Uniform not found: " + name);
        }
        return loc;
    }

    private static int compile(int type, String name) throws Exception {
        String path = "/assets/macclient/shaders/" + name;
        try (InputStream input = BloomRenderer.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new RuntimeException("Shader not found: " + path);
            }
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

    private static void deleteShader(int shader) {
        if (shader != -1) {
            GL20.glDeleteShader(shader);
        }
    }

    private static void drawQuad() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    private static void resize(int sw, int sh, int w, int h) {
        deleteBuffers();
        smallW = sw;
        smallH = sh;
        fullW = w;
        fullH = h;

        fxTex = createTexture(w, h);
        fxFbo = createFBO(fxTex);
        texA = createTexture(sw, sh);
        texB = createTexture(sw, sh);
        fboA = createFBO(texA);
        fboB = createFBO(texB);
    }

    private static int createTexture(int w, int h) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null
        );
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
        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, texture, 0
        );
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[MacClient] Bloom FBO incomplete: 0x" + Integer.toHexString(status));
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return fbo;
    }

    private static void deleteBuffers() {
        if (fxFbo != -1) {
            GL30.glDeleteFramebuffers(fxFbo);
            fxFbo = -1;
        }
        if (fboA != -1) {
            GL30.glDeleteFramebuffers(fboA);
            fboA = -1;
        }
        if (fboB != -1) {
            GL30.glDeleteFramebuffers(fboB);
            fboB = -1;
        }
        if (fxTex != -1) {
            GL11.glDeleteTextures(fxTex);
            fxTex = -1;
        }
        if (texA != -1) {
            GL11.glDeleteTextures(texA);
            texA = -1;
        }
        if (texB != -1) {
            GL11.glDeleteTextures(texB);
            texB = -1;
        }
    }

    private static final class GLState {
        private final int framebuffer;
        private final int readFramebuffer;
        private final int drawFramebuffer;
        private final int program;
        private final int vaoBound;
        private final int activeTexture;
        private final int texture2D;
        private final int[] viewport = new int[4];
        private final boolean depthTest;
        private final boolean blend;
        private final boolean cullFace;
        private final boolean depthMask;

        private GLState() {
            framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            vaoBound = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            texture2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        }

        private static GLState capture() {
            return new GLState();
        }

        private void restore() {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vaoBound);
            GL13.glActiveTexture(activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2D);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            setEnabled(GL11.GL_DEPTH_TEST, depthTest);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_CULL_FACE, cullFace);
            GL11.glDepthMask(depthMask);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }
}
