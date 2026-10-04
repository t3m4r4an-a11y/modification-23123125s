package net.macos.client.render;

import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Soft SDF outer glow around rounded rectangles.
 * Stateless — no FBO, no per-frame state. Draw before the panel; panel sits on top.
 *
 * Coordinates are in scaled GUI space (DrawContext coords), top-left origin.
 */
public final class GlowRenderer {

    private GlowRenderer() {}

    private static int program = -1;
    private static int vao = -1;
    private static int vbo = -1;

    private static int uTransform = -1;
    private static int uQuadSize = -1;
    private static int uRectSize = -1;
    private static int uRadius = -1;
    private static int uGlowSize = -1;
    private static int uColor = -1;

    private static boolean initialized = false;

    public static void draw(int x, int y, int w, int h, int radius,
                            float glowSize, int color) {
        if (w <= 0 || h <= 0 || glowSize <= 0.5f) return;

        int alpha = (color >>> 24) & 0xFF;
        if (alpha <= 2) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        try {
            ensureInitialized();
        } catch (Throwable t) {
            System.err.println("[Aetherion] GlowRenderer init failed:");
            t.printStackTrace();
            return;
        }

        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        int guiW = mc.getWindow().getScaledWidth();
        int guiH = mc.getWindow().getScaledHeight();
        if (fbW <= 0 || fbH <= 0 || guiW <= 0 || guiH <= 0) return;

        float sx = (float) fbW / guiW;
        float sy = (float) fbH / guiH;

        // Quad in GUI coords = rect + glow padding
        float gx = x - glowSize;
        float gy = y - glowSize;
        float gw = w + glowSize * 2f;
        float gh = h + glowSize * 2f;

        // → framebuffer pixel coords (bottom-left origin)
        float fbX = gx * sx;
        float fbY = (guiH - gy - gh) * sy;
        float fbWq = gw * sx;
        float fbHq = gh * sy;

        // Build NDC transform
        float ndcX = (fbX / fbW) * 2f - 1f;
        float ndcY = (fbY / fbH) * 2f - 1f;
        float ndcW = (fbWq / fbW) * 2f;
        float ndcH = (fbHq / fbH) * 2f;

        Matrix4f xform = new Matrix4f()
                .translate(ndcX + ndcW * 0.5f, ndcY + ndcH * 0.5f, 0f)
                .scale(ndcW * 0.5f, ndcH * 0.5f, 1f);

        // Uniform values in the quad's local pixel space
        float localQuadW = fbWq;
        float localQuadH = fbHq;
        float localRectW = w * sx;
        float localRectH = h * sy;
        float localRadius = radius * ((sx + sy) * 0.5f);
        float localGlow = glowSize * ((sx + sy) * 0.5f);

        float cr = ((color >> 16) & 0xFF) / 255f;
        float cg = ((color >> 8) & 0xFF) / 255f;
        float cb = (color & 0xFF) / 255f;
        float ca = alpha / 255f;

        // Save GL state we touch
        boolean wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            // Additive glow — multiple toasts don't darken each other
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            GL20.glUseProgram(program);

            float[] mat = new float[16];
            xform.get(mat);
            GL20.glUniformMatrix4fv(uTransform, false, mat);

            GL20.glUniform2f(uQuadSize, localQuadW, localQuadH);
            GL20.glUniform2f(uRectSize, localRectW, localRectH);
            GL20.glUniform1f(uRadius, localRadius);
            GL20.glUniform1f(uGlowSize, localGlow);
            GL20.glUniform4f(uColor, cr, cg, cb, ca);

            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            GL30.glBindVertexArray(0);

        } finally {
            GL20.glUseProgram(prevProgram);
            GL30.glBindVertexArray(prevVao);
            setEnabled(GL11.GL_DEPTH_TEST, wasDepth);
            setEnabled(GL11.GL_CULL_FACE, wasCull);
            setEnabled(GL11.GL_BLEND, wasBlend);
            if (wasBlend) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    private static void setEnabled(int cap, boolean enabled) {
        if (enabled) GL11.glEnable(cap);
        else         GL11.glDisable(cap);
    }

    private static void ensureInitialized() throws Exception {
        if (initialized && program != -1) return;

        int vs = compile(GL20.GL_VERTEX_SHADER, "glow.vsh");
        int fs = compile(GL20.GL_FRAGMENT_SHADER, "glow.fsh");

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "UV0");
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Glow link: " + GL20.glGetProgramInfoLog(program));
        }

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        uTransform = requireUniform("Transform");
        uQuadSize  = requireUniform("QuadSize");
        uRectSize  = requireUniform("RectSize");
        uRadius    = requireUniform("Radius");
        uGlowSize  = requireUniform("GlowSize");
        uColor     = requireUniform("GlowColor");

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

        GL30.glBindVertexArray(0);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);

        initialized = true;
    }

    private static int requireUniform(String name) {
        int loc = GL20.glGetUniformLocation(program, name);
        if (loc < 0) throw new RuntimeException("Glow uniform not found: " + name);
        return loc;
    }

    private static int compile(int type, String name) throws Exception {
        String path = "/assets/macclient/shaders/" + name;
        try (InputStream in = GlowRenderer.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing shader: " + path);
            String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, src);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                throw new RuntimeException(name + ":\n" + log);
            }
            return shader;
        }
    }
}