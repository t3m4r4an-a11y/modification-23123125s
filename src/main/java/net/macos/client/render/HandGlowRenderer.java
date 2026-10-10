package net.macos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.macos.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.Color;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Shader-based Hand & Item Chams Glow.
 *
 * Algorithm (ported from Phantom ShaderHandsRenderer):
 *  1. captureBeforeHands()  — blit main FBO into beforeFBO
 *  2. [vanilla hand/item render happens]
 *  3. captureAfterHands()   — blit main FBO into afterFBO
 *  4. renderOverlayIfPending() — called from HudRenderCallback:
 *       a. hands_mask_diff  : compare depth, extract silhouette into maskFBO
 *       b. Kawase blur      : blur the mask → blurFBOs
 *       c. hands_glow       : additive glow halo (blurredMask outside silhouette) → main FBO
 *       d. hands_overlay    : semi-transparent fill on silhouette (Glow mode only)
 *     OR:
 *       e. hands_block_overlay : procedural shader (Waves/Plasma/etc.)
 */
public final class HandGlowRenderer {

    private HandGlowRenderer() {}

    private static final HandGlowRenderer INSTANCE = new HandGlowRenderer();
    public static HandGlowRenderer getInstance() { return INSTANCE; }

    // ─── GL programs (lazily compiled) ────────────────────────────────────
    private int prog_mask_diff    = -1;
    private int prog_kawase_down  = -1;
    private int prog_kawase_up    = -1;
    private int prog_glow         = -1;
    private int prog_overlay      = -1;
    private int prog_block        = -1;

    // ─── VAO / VBO for fullscreen quad ────────────────────────────────────
    private int vao = -1;
    private int vbo = -1;

    // ─── Framebuffers ─────────────────────────────────────────────────────
    private Framebuffer beforeFBO;
    private Framebuffer afterFBO;
    private Framebuffer maskFBO;
    private final List<int[]> blurFBOs = new ArrayList<>(); // [fbo, tex, w, h]

    private int fbWidth  = -1;
    private int fbHeight = -1;

    // ─── State ────────────────────────────────────────────────────────────
    private boolean beforeCaptured = false;
    private boolean overlayPending = false;
    private boolean initialized    = false;

    private int cachedBeforeDepth = -1;
    private int cachedAfterDepth  = -1;

    // ═════════════════════════════════════════════════════════════════════
    // Public API (called from mixins / event hooks)
    // ═════════════════════════════════════════════════════════════════════

    public void captureBeforeHands() {
        if (!shouldRender()) { invalidate(); return; }
        try {
            ensureReady();
            blitMainFBO(getAttachmentFBO(beforeFBO));
            beforeCaptured = true;
        } catch (Exception e) {
            System.err.println("[MacClient][HandGlow] captureBeforeHands failed: " + e.getMessage());
            invalidate();
        }
    }

    public void captureAfterHands() {
        if (!shouldRender() || !beforeCaptured) { invalidate(); return; }
        try {
            ensureReady();
            blitMainFBO(getAttachmentFBO(afterFBO));
            overlayPending = true;
        } catch (Exception e) {
            System.err.println("[MacClient][HandGlow] captureAfterHands failed: " + e.getMessage());
            invalidate();
        }
    }

    /**
     * Called from HudRenderCallback — renders the glow/chams overlay onto the main FBO.
     */
    public void renderOverlayIfPending() {
        if (!overlayPending) return;
        if (!shouldRender()) { invalidate(); return; }
        try {
            ensureReady();
            renderOverlay();
        } catch (Exception e) {
            System.err.println("[MacClient][HandGlow] renderOverlay failed: " + e.getMessage());
        } finally {
            invalidate();
        }
    }

    public void invalidate() {
        beforeCaptured = false;
        overlayPending = false;
        cachedBeforeDepth = -1;
        cachedAfterDepth  = -1;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Core rendering
    // ═════════════════════════════════════════════════════════════════════

    private void renderOverlay() throws Exception {
        ConfigManager cfg = ConfigManager.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();

        // ── Wrap in GLStateGuard so colorMask, VAO, program, viewport, blend
        //    are ALWAYS fully restored even if an exception fires mid-pass ────
        try (var guard = GLStateGuard.push()) {

            // ── 1. Extract depth textures (configure for sampling) ──────────
            int depthBefore = beforeFBO.getDepthAttachment();
            int depthAfter  = afterFBO.getDepthAttachment();
            if (depthBefore != cachedBeforeDepth) { configureDepthSampler(depthBefore); cachedBeforeDepth = depthBefore; }
            if (depthAfter  != cachedAfterDepth)  { configureDepthSampler(depthAfter);  cachedAfterDepth  = depthAfter;  }

            // ── 2. Build mask ─────────────────────────────────────────────
            clearFBO(maskFBO);
            maskFBO.beginWrite(false);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();

            GL20.glUseProgram(prog_mask_diff);
            GL20.glUniform1i(GL20.glGetUniformLocation(prog_mask_diff, "DepthBefore"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(prog_mask_diff, "DepthAfter"),  2);
            bindTex(0, beforeFBO.getColorAttachment());
            bindTex(1, depthBefore);
            bindTex(2, depthAfter);
            drawQuad();
            unbindTex(0); unbindTex(1); unbindTex(2);

            RenderSystem.enableDepthTest();

            // ── 3. Parse settings ─────────────────────────────────────────
            float glowStrength = cfg.handChamsGlow ? cfg.handChamsGlowIntensity : 0f;
            float fillAmt      = cfg.handChamsAlpha;
            String mode        = cfg.handChamsMode;
            if (cfg.enableModelWetness && (!cfg.enableHandChams || "Wetness".equalsIgnoreCase(mode))) {
                mode = "Wetness";
                fillAmt = Math.max(0.65f, fillAmt);
            }

            float[] col1 = parseColor(cfg.handChamsColor);
            float[] col2 = parseColor(cfg.handChamsColor2);
            if (cfg.handChamsRainbow) {
                float t = (System.currentTimeMillis() % 100_000L) * cfg.handChamsRainbowSpeed * 8e-4f;
                col1 = hsvToRgb(t % 1f,            0.85f, 1f);
                col2 = hsvToRgb((t + 0.25f) % 1f,  0.85f, 1f);
            }

            // ── 4. Blur mask ──────────────────────────────────────────────
            int blurredTex = maskFBO.getColorAttachment();
            if (glowStrength > 0.001f) {
                float blurOffset = Math.max(0.1f, cfg.handChamsOutline);
                int levels = Math.max(3, Math.min(8, 3 + Math.round(cfg.handChamsOutline * 0.6f)));
                blurredTex = blurMask(levels, blurOffset);
            }

            // ── 5. Composite onto main FBO ────────────────────────────────
            main.beginWrite(false);
            RenderSystem.enableBlend();
            GL11.glColorMask(true, true, true, false);
            RenderSystem.disableDepthTest();

            // 5a. Glow halo (additive)
            if (glowStrength > 0.001f) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL20.glUseProgram(prog_glow);
                setUniform3f(prog_glow, "color",    col1[0], col1[1], col1[2]);
                setUniform3f(prog_glow, "color2",   col2[0], col2[1], col2[2]);
                setUniform1f(prog_glow, "exposure", Math.min(1.6f, 0.4f + glowStrength * 0.5f));
                GL20.glUniform1i(GL20.glGetUniformLocation(prog_glow, "BlurredTex"), 0);
                GL20.glUniform1i(GL20.glGetUniformLocation(prog_glow, "MaskTex"),    1);
                bindTex(0, blurredTex);
                bindTex(1, maskFBO.getColorAttachment());
                drawQuad();
                unbindTex(0); unbindTex(1);
            }

            // 5b. Fill / procedural overlay
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
            if (mode.equals("Glow")) {
                if (fillAmt > 0.001f) {
                    GL20.glUseProgram(prog_overlay);
                    setUniform3f(prog_overlay, "color",      col1[0], col1[1], col1[2]);
                    setUniform1f(prog_overlay, "fillAmount", fillAmt);
                    setUniform1f(prog_overlay, "alpha",      fillAmt);
                    GL20.glUniform1i(GL20.glGetUniformLocation(prog_overlay, "MaskTex"), 0);
                    bindTex(0, maskFBO.getColorAttachment());
                    drawQuad();
                    unbindTex(0);
                }
            } else {
                float shaderType = modeToFloat(mode);
                int fw = mc.getWindow().getFramebufferWidth();
                int fh = mc.getWindow().getFramebufferHeight();
                GL20.glUseProgram(prog_block);
                GL20.glUniform1i(GL20.glGetUniformLocation(prog_block, "MaskTex"), 0);
                setUniform2f(prog_block, "texelSize",  1f / Math.max(1, fw), 1f / Math.max(1, fh));
                setUniform3f(prog_block, "color",      col1[0], col1[1], col1[2]);
                setUniform3f(prog_block, "color2",     col2[0], col2[1], col2[2]);
                setUniform1f(prog_block, "time",       (System.currentTimeMillis() % 100_000L) / 1000f);
                setUniform1f(prog_block, "speed",      cfg.handChamsSpeed);
                setUniform1f(prog_block, "scale",      cfg.handChamsScale);
                setUniform1f(prog_block, "outline",    cfg.handChamsOutline);
                setUniform1f(prog_block, "glowStrength", glowStrength);
                setUniform1f(prog_block, "fill",       fillAmt);
                setUniform1f(prog_block, "alpha",      fillAmt);
                setUniform1f(prog_block, "shaderType", shaderType);
                setUniform1f(prog_block, "shaderIntensity", cfg.handChamsShaderIntensity);
                bindTex(0, maskFBO.getColorAttachment());
                drawQuad();
                unbindTex(0);
            }

            // ── 6. guard.close() automatically restores ALL state ─────────
            //      (colorMask, blendFunc, program, VAO, viewport, FBO etc.)
        }

        // Re-enter Minecraft's main FBO cleanly for subsequent HUD rendering
        GLStateGuard.returnToMainFbo();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Kawase blur
    // ═════════════════════════════════════════════════════════════════════

    private int blurMask(int levels, float offset) {
        ensureBlurLevels(levels);
        if (blurFBOs.isEmpty()) return maskFBO.getColorAttachment();

        int colorTex = maskFBO.getColorAttachment();

        // Downsample
        for (int i = 0; i < levels; i++) {
            int[] lvl = blurFBOs.get(i);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lvl[0]);
            GL11.glViewport(0, 0, lvl[2], lvl[3]);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            GL20.glUseProgram(prog_kawase_down);
            GL20.glUniform1i(GL20.glGetUniformLocation(prog_kawase_down, "MaskTex"), 0);
            setUniform2f(prog_kawase_down, "uOffset",    (1 + i) * offset, (1 + i) * offset);
            setUniform2f(prog_kawase_down, "uHalfPixel", 0.5f / lvl[2], 0.5f / lvl[3]);
            bindTex(0, colorTex);
            drawQuad();
            unbindTex(0);

            colorTex = lvl[1];
        }

        // Upsample
        for (int i = levels - 1; i >= 1; i--) {
            int[] lvl = blurFBOs.get(i - 1);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, lvl[0]);
            GL11.glViewport(0, 0, lvl[2], lvl[3]);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            GL20.glUseProgram(prog_kawase_up);
            GL20.glUniform1i(GL20.glGetUniformLocation(prog_kawase_up, "MaskTex"), 0);
            setUniform2f(prog_kawase_up, "uOffset",    (1 + i) * offset, (1 + i) * offset);
            setUniform2f(prog_kawase_up, "uHalfPixel", 0.5f / lvl[2], 0.5f / lvl[3]);
            setUniform3f(prog_kawase_up, "glowColor",  1f, 1f, 1f);
            bindTex(0, colorTex);
            drawQuad();
            unbindTex(0);

            colorTex = lvl[1];
        }

        // Restore main FBO viewport
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.getFramebuffer().beginWrite(true);
        return colorTex;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Init / resize helpers
    // ═════════════════════════════════════════════════════════════════════

    private void ensureReady() throws Exception {
        if (!initialized) {
            initShaders();
            initQuad();
            initialized = true;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        if (fw != fbWidth || fh != fbHeight || beforeFBO == null || afterFBO == null || maskFBO == null) {
            deleteFBOs();
            beforeFBO = new SimpleFramebuffer(fw, fh, true, false);
            afterFBO  = new SimpleFramebuffer(fw, fh, true, false);
            maskFBO   = new SimpleFramebuffer(fw, fh, true, false);
            fbWidth = fw; fbHeight = fh;
        }
    }

    private void ensureBlurLevels(int count) {
        // Remove extra levels
        while (blurFBOs.size() > count) {
            int[] old = blurFBOs.remove(blurFBOs.size() - 1);
            GL30.glDeleteFramebuffers(old[0]);
            GL11.glDeleteTextures(old[1]);
        }
        // Add missing levels
        for (int i = blurFBOs.size(); i < count; i++) {
            int w = Math.max(2, fbWidth  >> (i + 1));
            int h = Math.max(2, fbHeight >> (i + 1));
            int tex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            int fbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            blurFBOs.add(new int[]{fbo, tex, w, h});
        }
    }

    private void deleteFBOs() {
        if (beforeFBO != null) { beforeFBO.delete(); beforeFBO = null; }
        if (afterFBO  != null) { afterFBO.delete();  afterFBO  = null; }
        if (maskFBO   != null) { maskFBO.delete();   maskFBO   = null; }
        for (int[] lvl : blurFBOs) {
            GL30.glDeleteFramebuffers(lvl[0]);
            GL11.glDeleteTextures(lvl[1]);
        }
        blurFBOs.clear();
    }

    // ═════════════════════════════════════════════════════════════════════
    // GL helpers
    // ═════════════════════════════════════════════════════════════════════

    private void blitMainFBO(int destFbo) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int mainFbo = mc.getFramebuffer().fbo;
        int prevRead = GL11.glGetInteger(0x8CA3); // GL_READ_FRAMEBUFFER_BINDING
        int prevDraw = GL11.glGetInteger(0x8CA6); // GL_DRAW_FRAMEBUFFER_BINDING
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFbo);
        GL30.glBlitFramebuffer(0, 0, fbWidth, fbHeight, 0, 0, fbWidth, fbHeight,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
        mc.getFramebuffer().beginWrite(true);
    }

    private int getAttachmentFBO(Framebuffer fb) { return fb.fbo; }

    private void configureDepthSampler(int texId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 34892, 0);      // GL_TEXTURE_COMPARE_MODE = GL_NONE
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void clearFBO(Framebuffer fb) {
        fb.setClearColor(0f, 0f, 0f, 0f);
        fb.clear(false);
    }

    private void bindTex(int slot, int tex) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + slot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
    }

    private void unbindTex(int slot) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + slot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void drawQuad() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    private void setUniform1f(int prog, String name, float v) {
        int loc = GL20.glGetUniformLocation(prog, name);
        if (loc >= 0) GL20.glUniform1f(loc, v);
    }

    private void setUniform2f(int prog, String name, float x, float y) {
        int loc = GL20.glGetUniformLocation(prog, name);
        if (loc >= 0) GL20.glUniform2f(loc, x, y);
    }

    private void setUniform3f(int prog, String name, float x, float y, float z) {
        int loc = GL20.glGetUniformLocation(prog, name);
        if (loc >= 0) GL20.glUniform3f(loc, x, y, z);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Shader compilation
    // ═════════════════════════════════════════════════════════════════════

    private void initShaders() throws Exception {
        String vsh = loadShader("hands_common.vsh");
        prog_mask_diff   = linkProg(vsh, loadShader("hands_mask_diff.fsh"),   "hands_mask_diff");
        prog_kawase_down = linkProg(vsh, loadShader("hands_kawase_down.fsh"),  "hands_kawase_down");
        prog_kawase_up   = linkProg(vsh, loadShader("hands_kawase_up.fsh"),    "hands_kawase_up");
        prog_glow        = linkProg(vsh, loadShader("hands_glow.fsh"),         "hands_glow");
        prog_overlay     = linkProg(vsh, loadShader("hands_overlay.fsh"),      "hands_overlay");
        prog_block       = linkProg(vsh, loadShader("hands_block_overlay.fsh"),"hands_block_overlay");
    }

    private String loadShader(String name) throws Exception {
        String path = "/assets/macclient/shaders/" + name;
        try (InputStream is = HandGlowRenderer.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Shader not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int linkProg(String vshSrc, String fshSrc, String debugName) throws Exception {
        int vs = compileShader(GL20.GL_VERTEX_SHADER,   vshSrc, debugName + ".vsh");
        int fs = compileShader(GL20.GL_FRAGMENT_SHADER, fshSrc, debugName + ".fsh");
        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glBindAttribLocation(prog, 0, "Position");
        GL20.glBindAttribLocation(prog, 1, "UV0");
        GL20.glLinkProgram(prog);
        if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(debugName + " link error: " + GL20.glGetProgramInfoLog(prog));
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String src, String debugName) throws Exception {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(debugName + " compile error: " + GL20.glGetShaderInfoLog(s));
        return s;
    }

    private void initQuad() {
        // NDC fullscreen triangle pair — UV (0,0)=bottom-left to (1,1)=top-right
        float[] verts = {
            -1f, -1f,  0f, 0f,
             1f, -1f,  1f, 0f,
             1f,  1f,  1f, 1f,
            -1f, -1f,  0f, 0f,
             1f,  1f,  1f, 1f,
            -1f,  1f,  0f, 1f,
        };
        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL30.glBufferData(GL15.GL_ARRAY_BUFFER, verts, GL15.GL_STATIC_DRAW);
        int stride = 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);
        GL30.glBindVertexArray(0);
        GL30.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Utility
    // ═════════════════════════════════════════════════════════════════════

    private boolean shouldRender() {
        ConfigManager cfg = ConfigManager.INSTANCE;
        if (!cfg.enableHandChams && !cfg.enableItemChams) return false;
        return cfg.handChamsGlow || cfg.handChamsAlpha > 0.001f;
    }

    private float modeToFloat(String mode) {
        return switch (mode) {
            case "Waves"     -> 0f;
            case "Plasma"    -> 1f;
            case "Cyberpunk" -> 2f;
            case "Fire"      -> 3f;
            case "Lightning" -> 4f;
            case "Rainbow"   -> 5f;
            case "Aurora"    -> 6f;
            case "Wetness"   -> 7f;
            default          -> 0f;
        };
    }

    private float[] parseColor(String hex) {
        try {
            int c = Integer.parseInt(hex.replace("#", ""), 16);
            return new float[]{ ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f };
        } catch (Exception e) {
            return new float[]{ 0f, 0.83f, 1f };
        }
    }

    private float[] hsvToRgb(float h, float s, float v) {
        int rgb = Color.HSBtoRGB(h, s, v);
        return new float[]{ ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f };
    }
}
