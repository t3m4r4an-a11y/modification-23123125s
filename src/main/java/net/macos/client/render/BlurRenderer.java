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
import org.lwjgl.opengl.GL12;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class BlurRenderer {

    private BlurRenderer() {
    }

    /*
     * ============================================================
     * CONFIG
     * ============================================================
     */

    private static final int MAX_RADIUS = 32;

    /*
     * HUD background capture is rendered at 1/4 resolution.
     * This gives us a much cheaper blur while still looking smooth.
     */
    private static final int CAP_DOWNSAMPLE = 4;

    /*
     * Fixed radius for the background captured behind GlassWidget.
     * The actual GUI panel then samples this already blurred texture.
     */
    private static final int CAP_RADIUS = 12;

    /*
     * ============================================================
     * SHADER
     * ============================================================
     */

    private static int program = -1;
    private static int vao = -1;
    private static int vbo = -1;

    private static int uDiffuse = -1;
    private static int uBlurDir = -1;
    private static int uRadius = -1;

    /*
     * ============================================================
     * FULLSCREEN FBO
     * ============================================================
     */

    private static int fboA = -1;
    private static int fboB = -1;

    private static int texA = -1;
    private static int texB = -1;

    private static int width = 0;
    private static int height = 0;

    /*
     * ============================================================
     * HUD CAPTURE FBO
     * ============================================================
     */

    private static int capFboA = -1;
    private static int capFboB = -1;

    private static int capTexA = -1;
    private static int capTexB = -1;

    private static int capSmallW = 0;
    private static int capSmallH = 0;

    private static int capFullW = 0;
    private static int capFullH = 0;

    /*
     * We don't permanently disable the renderer after one error.
     * Minecraft can recreate the GL context, resize the window,
     * or recover from a transient framebuffer problem.
     */
    private static boolean initialized = false;

    /*
     * ============================================================
     * FULLSCREEN BLUR
     * ============================================================
     */

    public static void applyFullscreen(int radius) {
        if (!ConfigManager.INSTANCE.enableGlassBlur) {
            return;
        }

        radius = clampRadius(radius);

        if (radius <= 0) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) {
            return;
        }

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        try {
            ensureInitialized();

            if (fboA == -1 || fboB == -1 || width != w || height != h) {
                resize(w, h);
            }

            int mainFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

            GLState state = GLState.capture();

            try {
                /*
                 * We are rendering a fullscreen post-process.
                 * Depth, culling and blending are not required.
                 */
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_CULL_FACE);

                /*
                 * ----------------------------------------------------
                 * PASS 1
                 * Main framebuffer -> texture A
                 * ----------------------------------------------------
                 */
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fboA);

                GL30.glBlitFramebuffer(
                        0,
                        0,
                        w,
                        h,
                        0,
                        0,
                        w,
                        h,
                        GL11.GL_COLOR_BUFFER_BIT,
                        GL11.GL_NEAREST
                );

                /*
                 * ----------------------------------------------------
                 * PASS 2
                 * Horizontal blur: A -> B
                 * ----------------------------------------------------
                 */
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

                /*
                 * ----------------------------------------------------
                 * PASS 3
                 * Vertical blur: B -> A
                 * ----------------------------------------------------
                 */
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboA);

                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);

                GL20.glUniform2f(uBlurDir, 0f, 1f);

                drawQuad();

                /*
                 * ----------------------------------------------------
                 * PASS 4
                 * A -> Minecraft framebuffer
                 * ----------------------------------------------------
                 */
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fboA);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);

                GL30.glBlitFramebuffer(
                        0,
                        0,
                        w,
                        h,
                        0,
                        0,
                        w,
                        h,
                        GL11.GL_COLOR_BUFFER_BIT,
                        GL11.GL_NEAREST
                );

            } finally {
                state.restore();

                /*
                 * Fabric/Minecraft expects its framebuffer to be bound
                 * for subsequent rendering.
                 */
                mc.getFramebuffer().beginWrite(false);

                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

        } catch (Throwable t) {
            System.err.println("[MacClient] Blur fullscreen failed:");
            t.printStackTrace();
        }
    }

    /*
     * ============================================================
     * CAPTURE BLURRED BACKGROUND
     * ============================================================
     *
     * This is used by GlassWidget.
     *
     * Full framebuffer
     *      ↓
     * 1/4 resolution
     *      ↓
     * horizontal blur
     *      ↓
     * vertical blur
     *      ↓
     * capTexA
     */

    public static void captureBlurredBackground() {
        if (!ConfigManager.INSTANCE.enableGlassBlur) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.getWindow() == null || mc.getFramebuffer() == null) {
            return;
        }

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        try {
            ensureInitialized();

            int sw = Math.max(1, w / CAP_DOWNSAMPLE);
            int sh = Math.max(1, h / CAP_DOWNSAMPLE);

            if (
                    capFboA == -1 ||
                            capFboB == -1 ||
                            capSmallW != sw ||
                            capSmallH != sh
            ) {
                resizeCapture(sw, sh);
            }

            capFullW = w;
            capFullH = h;

            int mainFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

            GLState state = GLState.capture();

            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_CULL_FACE);

                /*
                 * ----------------------------------------------------
                 * PASS 1
                 * Full resolution -> 1/4 resolution
                 *
                 * LINEAR filtering gives us a cheap pre-blur while
                 * downsampling.
                 * ----------------------------------------------------
                 */
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, capFboA);

                GL30.glBlitFramebuffer(
                        0,
                        0,
                        w,
                        h,
                        0,
                        0,
                        sw,
                        sh,
                        GL11.GL_COLOR_BUFFER_BIT,
                        GL11.GL_LINEAR
                );

                /*
                 * ----------------------------------------------------
                 * PASS 2
                 * Horizontal blur
                 * ----------------------------------------------------
                 */
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, capFboB);

                GL11.glViewport(0, 0, sw, sh);
                GL11.glClearColor(0f, 0f, 0f, 0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                GL20.glUseProgram(program);

                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, capTexA);

                GL20.glUniform1i(uDiffuse, 0);
                GL20.glUniform2f(uBlurDir, 1f, 0f);
                GL20.glUniform1f(uRadius, CAP_RADIUS);

                drawQuad();

                /*
                 * ----------------------------------------------------
                 * PASS 3
                 * Vertical blur
                 * ----------------------------------------------------
                 */
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, capFboA);

                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, capTexB);

                GL20.glUniform2f(uBlurDir, 0f, 1f);

                drawQuad();

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
            System.err.println("[MacClient] Blur background capture failed:");
            t.printStackTrace();
        }
    }

    /*
     * ============================================================
     * DRAW BLURRED REGION
     * ============================================================
     */

    public static void drawBlurredRegion(
            DrawContext ctx,
            int x,
            int y,
            int w,
            int h
    ) {
        if (capTexA == -1) {
            System.out.println("[MacClient] drawBlurredRegion: capTexA = -1");
            return;
        }

        if (w <= 0 || h <= 0) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.getWindow() == null) {
            return;
        }

        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();

        int guiW = mc.getWindow().getScaledWidth();
        int guiH = mc.getWindow().getScaledHeight();

        if (fbW <= 0 || fbH <= 0 || guiW <= 0 || guiH <= 0) {
            return;
        }

        /*
         * GUI -> framebuffer
         */
        float sx = (float) fbW / (float) guiW;
        float sy = (float) fbH / (float) guiH;

        int fx = Math.round(x * sx);
        int fy = Math.round(y * sy);
        int fw = Math.max(1, Math.round(w * sx));
        int fh = Math.max(1, Math.round(h * sy));

        /*
         * Framebuffer -> normalized UV.
         *
         * capTexA represents the complete framebuffer,
         * just at 1/4 resolution.
         */
        float u0 = (float) fx / (float) fbW;
        float u1 = (float) (fx + fw) / (float) fbW;

        float v0 = 1.0f - (float) (fy + fh) / (float) fbH;
        float v1 = 1.0f - (float) fy / (float) fbH;

        System.out.println(
                "[MacClient] Blur region: " +
                        "tex=" + capTexA +
                        " gui=" + x + "," + y + "," + w + "," + h +
                        " fb=" + fx + "," + fy + "," + fw + "," + fh +
                        " uv=" + u0 + "," + v0 + " -> " + u1 + "," + v1
        );

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, capTexA);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix =
                ctx.getMatrices().peek().getPositionMatrix();

        BufferBuilder buffer =
                Tessellator.getInstance().getBuffer();

        buffer.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE
        );

        buffer.vertex(matrix, x, y, 0)
                .texture(u0, v1)
                .next();

        buffer.vertex(matrix, x + w, y, 0)
                .texture(u1, v1)
                .next();

        buffer.vertex(matrix, x + w, y + h, 0)
                .texture(u1, v0)
                .next();

        buffer.vertex(matrix, x, y + h, 0)
                .texture(u0, v0)
                .next();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
    }

    /*
     * ============================================================
     * INITIALIZATION
     * ============================================================
     */

    private static void ensureInitialized() throws Exception {
        if (initialized && program != -1 && vao != -1 && vbo != -1) {
            return;
        }

        init();

        initialized = true;
    }

    private static void init() throws Exception {
        int vertexShader = -1;
        int fragmentShader = -1;

        try {
            vertexShader = compile(
                    GL20.GL_VERTEX_SHADER,
                    "blur.vsh"
            );

            fragmentShader = compile(
                    GL20.GL_FRAGMENT_SHADER,
                    "blur.fsh"
            );

            program = GL20.glCreateProgram();

            GL20.glAttachShader(program, vertexShader);
            GL20.glAttachShader(program, fragmentShader);

            /*
             * Explicitly bind attribute locations.
             *
             * This makes:
             * location 0 = Position
             * location 1 = UV0
             *
             * instead of relying on the driver assigning locations.
             */
            GL20.glBindAttribLocation(
                    program,
                    0,
                    "Position"
            );

            GL20.glBindAttribLocation(
                    program,
                    1,
                    "UV0"
            );

            GL20.glLinkProgram(program);

            if (
                    GL20.glGetProgrami(
                            program,
                            GL20.GL_LINK_STATUS
                    ) == GL11.GL_FALSE
            ) {
                throw new RuntimeException(
                        "Blur shader link failed: "
                                + GL20.glGetProgramInfoLog(program)
                );
            }

            uDiffuse =
                    GL20.glGetUniformLocation(
                            program,
                            "DiffuseSampler"
                    );

            uBlurDir =
                    GL20.glGetUniformLocation(
                            program,
                            "BlurDir"
                    );

            uRadius =
                    GL20.glGetUniformLocation(
                            program,
                            "Radius"
                    );

            if (uDiffuse < 0) {
                throw new RuntimeException(
                        "Blur shader uniform DiffuseSampler not found"
                );
            }

            if (uBlurDir < 0) {
                throw new RuntimeException(
                        "Blur shader uniform BlurDir not found"
                );
            }

            if (uRadius < 0) {
                throw new RuntimeException(
                        "Blur shader uniform Radius not found"
                );
            }

            /*
             * Fullscreen triangle pair.
             *
             * Position.xy
             * UV.xy
             */
            float[] vertices = {
                    -1f, -1f, 0f, 0f,
                    1f, -1f, 1f, 0f,
                    1f,  1f, 1f, 1f,

                    -1f, -1f, 0f, 0f,
                    1f,  1f, 1f, 1f,
                    -1f,  1f, 0f, 1f
            };

            vao = GL30.glGenVertexArrays();
            vbo = GL30.glGenBuffers();

            GL30.glBindVertexArray(vao);

            GL30.glBindBuffer(
                    GL30.GL_ARRAY_BUFFER,
                    vbo
            );

            GL30.glBufferData(
                    GL30.GL_ARRAY_BUFFER,
                    vertices,
                    GL30.GL_STATIC_DRAW
            );

            int stride = 4 * Float.BYTES;

            GL20.glEnableVertexAttribArray(0);

            GL20.glVertexAttribPointer(
                    0,
                    2,
                    GL11.GL_FLOAT,
                    false,
                    stride,
                    0
            );

            GL20.glEnableVertexAttribArray(1);

            GL20.glVertexAttribPointer(
                    1,
                    2,
                    GL11.GL_FLOAT,
                    false,
                    stride,
                    2L * Float.BYTES
            );

            GL30.glBindVertexArray(0);
            GL30.glBindBuffer(
                    GL30.GL_ARRAY_BUFFER,
                    0
            );

            System.out.println(
                    "[MacClient] BlurRenderer initialized"
            );

        } finally {
            if (vertexShader != -1) {
                GL20.glDeleteShader(vertexShader);
            }

            if (fragmentShader != -1) {
                GL20.glDeleteShader(fragmentShader);
            }
        }
    }

    /*
     * ============================================================
     * SHADER COMPILATION
     * ============================================================
     */

    private static int compile(
            int type,
            String name
    ) throws Exception {

        String path =
                "/assets/macclient/shaders/" + name;

        try (
                InputStream input =
                        BlurRenderer.class.getResourceAsStream(path)
        ) {
            if (input == null) {
                throw new RuntimeException(
                        "Shader not found: " + path
                );
            }

            String source =
                    new String(
                            input.readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            int shader =
                    GL20.glCreateShader(type);

            GL20.glShaderSource(
                    shader,
                    source
            );

            GL20.glCompileShader(shader);

            if (
                    GL20.glGetShaderi(
                            shader,
                            GL20.GL_COMPILE_STATUS
                    ) == GL11.GL_FALSE
            ) {
                String log =
                        GL20.glGetShaderInfoLog(shader);

                GL20.glDeleteShader(shader);

                throw new RuntimeException(
                        name + " compilation failed:\n" + log
                );
            }

            return shader;
        }
    }

    /*
     * ============================================================
     * QUAD
     * ============================================================
     */

    private static void drawQuad() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(
                GL11.GL_TRIANGLES,
                0,
                6
        );
        GL30.glBindVertexArray(0);
    }

    /*
     * ============================================================
     * FULLSCREEN FBO RESIZE
     * ============================================================
     */

    private static void resize(
            int w,
            int h
    ) {
        deleteFullscreenBuffers();

        width = w;
        height = h;

        texA = createTexture(w, h);
        texB = createTexture(w, h);

        fboA = createFBO(texA);
        fboB = createFBO(texB);
    }

    /*
     * ============================================================
     * CAPTURE FBO RESIZE
     * ============================================================
     */

    private static void resizeCapture(
            int w,
            int h
    ) {
        deleteCaptureBuffers();

        capSmallW = w;
        capSmallH = h;

        capTexA = createTexture(w, h);
        capTexB = createTexture(w, h);

        capFboA = createFBO(capTexA);
        capFboB = createFBO(capTexB);
    }

    /*
     * ============================================================
     * TEXTURE
     * ============================================================
     */

    private static int createTexture(
            int w,
            int h
    ) {
        int texture =
                GL11.glGenTextures();

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                texture
        );

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                w,
                h,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                (java.nio.ByteBuffer) null
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_LINEAR
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_LINEAR
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL11.GL_TEXTURE_2D,
                GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                0
        );

        return texture;
    }

    /*
     * ============================================================
     * FBO
     * ============================================================
     */

    private static int createFBO(
            int texture
    ) {
        int fbo =
                GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                fbo
        );

        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D,
                texture,
                0
        );

        int status =
                GL30.glCheckFramebufferStatus(
                        GL30.GL_FRAMEBUFFER
                );

        if (
                status !=
                        GL30.GL_FRAMEBUFFER_COMPLETE
        ) {
            System.err.println(
                    "[MacClient] Blur FBO incomplete: 0x"
                            + Integer.toHexString(status)
            );
        }

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                0
        );

        return fbo;
    }

    /*
     * ============================================================
     * CLEANUP
     * ============================================================
     */

    private static void deleteFullscreenBuffers() {
        if (fboA != -1) {
            GL30.glDeleteFramebuffers(fboA);
            fboA = -1;
        }

        if (fboB != -1) {
            GL30.glDeleteFramebuffers(fboB);
            fboB = -1;
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

    private static void deleteCaptureBuffers() {
        if (capFboA != -1) {
            GL30.glDeleteFramebuffers(capFboA);
            capFboA = -1;
        }

        if (capFboB != -1) {
            GL30.glDeleteFramebuffers(capFboB);
            capFboB = -1;
        }

        if (capTexA != -1) {
            GL11.glDeleteTextures(capTexA);
            capTexA = -1;
        }

        if (capTexB != -1) {
            GL11.glDeleteTextures(capTexB);
            capTexB = -1;
        }
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private static int clampRadius(int radius) {
        return Math.max(
                0,
                Math.min(
                        MAX_RADIUS,
                        radius
                )
        );
    }

    /*
     * ============================================================
     * OPENGL STATE SNAPSHOT
     * ============================================================
     */

    private static final class GLState {

        private final int framebuffer;
        private final int readFramebuffer;
        private final int drawFramebuffer;

        private final int program;
        private final int vao;

        private final int activeTexture;
        private final int texture2D;

        private final int[] viewport =
                new int[4];

        private final boolean depthTest;
        private final boolean blend;
        private final boolean cullFace;

        private final boolean depthMask;

        private GLState() {
            framebuffer =
                    GL11.glGetInteger(
                            GL30.GL_FRAMEBUFFER_BINDING
                    );

            readFramebuffer =
                    GL11.glGetInteger(
                            GL30.GL_READ_FRAMEBUFFER_BINDING
                    );

            drawFramebuffer =
                    GL11.glGetInteger(
                            GL30.GL_DRAW_FRAMEBUFFER_BINDING
                    );

            program =
                    GL11.glGetInteger(
                            GL20.GL_CURRENT_PROGRAM
                    );

            vao =
                    GL11.glGetInteger(
                            GL30.GL_VERTEX_ARRAY_BINDING
                    );

            activeTexture =
                    GL11.glGetInteger(
                            GL13.GL_ACTIVE_TEXTURE
                    );

            texture2D =
                    GL11.glGetInteger(
                            GL11.GL_TEXTURE_BINDING_2D
                    );

            GL11.glGetIntegerv(
                    GL11.GL_VIEWPORT,
                    viewport
            );

            depthTest =
                    GL11.glIsEnabled(
                            GL11.GL_DEPTH_TEST
                    );

            blend =
                    GL11.glIsEnabled(
                            GL11.GL_BLEND
                    );

            cullFace =
                    GL11.glIsEnabled(
                            GL11.GL_CULL_FACE
                    );

            depthMask =
                    GL11.glGetBoolean(
                            GL11.GL_DEPTH_WRITEMASK
                    );
        }

        private static GLState capture() {
            return new GLState();
        }

        private void restore() {
            /*
             * Framebuffers
             */
            GL30.glBindFramebuffer(
                    GL30.GL_READ_FRAMEBUFFER,
                    readFramebuffer
            );

            GL30.glBindFramebuffer(
                    GL30.GL_DRAW_FRAMEBUFFER,
                    drawFramebuffer
            );

            GL30.glBindFramebuffer(
                    GL30.GL_FRAMEBUFFER,
                    framebuffer
            );

            /*
             * Program / VAO
             */
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);

            /*
             * Texture state
             */
            GL13.glActiveTexture(activeTexture);
            GL11.glBindTexture(
                    GL11.GL_TEXTURE_2D,
                    texture2D
            );

            /*
             * Viewport
             */
            GL11.glViewport(
                    viewport[0],
                    viewport[1],
                    viewport[2],
                    viewport[3]
            );

            /*
             * Capability state
             */
            setEnabled(
                    GL11.GL_DEPTH_TEST,
                    depthTest
            );

            setEnabled(
                    GL11.GL_BLEND,
                    blend
            );

            setEnabled(
                    GL11.GL_CULL_FACE,
                    cullFace
            );

            GL11.glDepthMask(depthMask);
        }

        private static void setEnabled(
                int capability,
                boolean enabled
        ) {
            if (enabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }
}