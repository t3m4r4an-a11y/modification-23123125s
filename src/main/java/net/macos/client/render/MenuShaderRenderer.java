package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.MacClient;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Real-time GPU Shader Background for Aetherion GUI.
 * Renders an ethereal, dark liquid chromatic aurora mesh at 60+ FPS behind menus.
 */
public final class MenuShaderRenderer {

    private MenuShaderRenderer() {}

    private static boolean initialized = false;
    private static int program = -1;
    private static int vao = -1;
    private static int vbo = -1;

    private static int uResolution = -1;
    private static int uTime = -1;
    private static int uAccent = -1;
    private static int uAlpha = -1;

    private static final long START_TIME = System.currentTimeMillis();

    public static void init() {
        if (initialized) return;
        try {
            String vshSrc = loadResource("/assets/macclient/shaders/menu_mesh.vsh");
            String fshSrc = loadResource("/assets/macclient/shaders/menu_mesh.fsh");

            int vsh = compile(GL20.GL_VERTEX_SHADER, vshSrc, "menu_mesh.vsh");
            int fsh = compile(GL20.GL_FRAGMENT_SHADER, fshSrc, "menu_mesh.fsh");

            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vsh);
            GL20.glAttachShader(program, fsh);
            GL20.glLinkProgram(program);

            GL20.glDeleteShader(vsh);
            GL20.glDeleteShader(fsh);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program);
                throw new RuntimeException("Menu shader link failed: " + log);
            }

            uResolution = GL20.glGetUniformLocation(program, "resolution");
            uTime = GL20.glGetUniformLocation(program, "time");
            uAccent = GL20.glGetUniformLocation(program, "accent");
            uAlpha = GL20.glGetUniformLocation(program, "alpha");

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
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo);
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertices, GL30.GL_STATIC_DRAW);

            int stride = 4 * Float.BYTES;
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);

            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);

            initialized = true;
            MacClient.LOGGER.info("Aetherion Menu Shader ready");
        } catch (Throwable t) {
            MacClient.LOGGER.error("Failed to initialize Aetherion Menu Shader", t);
        }
    }

    public static void render(float alpha, int accentRgb) {
        if (alpha <= 0.005f) return;
        if (!initialized) init();
        if (!initialized) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        if (fw <= 0 || fh <= 0) return;

        float r = ((accentRgb >> 16) & 0xFF) / 255.0f;
        float g = ((accentRgb >> 8)  & 0xFF) / 255.0f;
        float b = (accentRgb & 0xFF)         / 255.0f;

        float timeSec = (System.currentTimeMillis() - START_TIME) / 1000.0f;

        try (var guard = GLStateGuard.push()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            GL20.glUseProgram(program);
            GL20.glUniform2f(uResolution, (float) fw, (float) fh);
            GL20.glUniform1f(uTime, timeSec);
            GL20.glUniform3f(uAccent, r, g, b);
            GL20.glUniform1f(uAlpha, alpha);

            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            GL30.glBindVertexArray(0);
            GL20.glUseProgram(0);
        } catch (Throwable t) {
            // Silently ignore if driver issues occur
        }
    }

    private static int compile(int type, String src, String name) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            throw new RuntimeException(name + " compilation failed: " + log);
        }
        return shader;
    }

    private static String loadResource(String path) throws Exception {
        try (InputStream in = MenuShaderRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
