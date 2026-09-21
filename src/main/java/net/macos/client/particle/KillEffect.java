package net.macos.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class KillEffect {

    private static final Random RNG = new Random();
    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final List<Proj> PROJECTIONS = new ArrayList<>();
    private static final long DURATION = 2000L;

    // === Ghost spritesheet ===
    private static final Identifier GHOST_TEXTURE =
        new Identifier("macclient", "textures/particle/ghost.png");
    private static final int GHOST_TEX_W = 1536;
    private static final int GHOST_TEX_H = 1024;
    private static final int GHOST_COLS = 11;
    private static final int GHOST_ROWS = 4;
    private static final int GHOST_TOTAL = 40;
    private static final int GHOST_FRAME_W = GHOST_TEX_W / GHOST_COLS;  // 139
    private static final int GHOST_FRAME_H = GHOST_TEX_H / GHOST_ROWS;  // 256

    private static class Effect {
        double x, y, z;
        long startTime;
        KillEffectType type;
        int color;
        long seed;
    }

    private static class Proj {
        int cx, cy;
        float size;
        float progress;
        KillEffectType type;
        int color;
        long seed;
        float drawHeight;   // ← НОВОЕ: точная высота в пикселях
    }

    public static void trigger(double x, double y, double z, KillEffectType type, String colorHex) {
        if (type == KillEffectType.NONE) return;

        if (EFFECTS.size() >= 3) EFFECTS.remove(0);   // максимум 3 одновременно

        Effect e = new Effect();
        e.x = x;
        e.y = y;
        e.z = z;
        e.startTime = System.currentTimeMillis();
        e.type = type;
        try {
            e.color = Color.decode(colorHex).getRGB() & 0xFFFFFF;
        } catch (Exception ex) {
            e.color = 0xFFD700;
        }
        e.seed = RNG.nextLong();
        EFFECTS.add(e);
    }

    // ============================================================
    // ПРОЕКЦИЯ
    // ============================================================
    public static void updateProjections(MatrixStack matrices, Camera camera, float tickDelta) {
        PROJECTIONS.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        Vec3d camPos = camera.getPos();

        Iterator<Effect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            long elapsed = now - e.startTime;
            if (elapsed > DURATION) {
                it.remove();
                continue;
            }

            double tx = e.x;
            double ty = e.y;
            double tz = e.z;

            // Смещения под тип
            switch (e.type) {
                case RING -> ty = e.y - 1.0;
                case GHOST -> ty = e.y + 0.8;   // над головой
                default -> {}
            }

            matrices.push();
            matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);
            Matrix4f mv = matrices.peek().getPositionMatrix();

            Vector4f clip = new Vector4f(0, 0, 0, 1);
            mv.transform(clip);
            projMat.transform(clip);
            matrices.pop();

            if (clip.w <= 0.001f) continue;

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;

            int sx = (int)((ndcX * 0.5f + 0.5f) * sw);
            int sy = (int)((1.0f - (ndcY * 0.5f + 0.5f)) * sh);

            double dist = Math.sqrt((tx - camPos.x) * (tx - camPos.x)
                    + (ty - camPos.y) * (ty - camPos.y)
                    + (tz - camPos.z) * (tz - camPos.z));

            float size = 80f / Math.max(1f, (float)dist);

            // Высота ghost в пикселях — через meters-per-pixel
            float fov = MinecraftClient.getInstance().options.getFov().getValue();
            double mpp = sh / (2.0 * Math.tan(Math.toRadians(fov / 2.0)) * Math.max(0.5, dist));
            float ghostHeight = (float)(2.0 * mpp);   // ~2 блока = высота хитбокса

            Proj p = new Proj();
            p.cx = sx;
            p.cy = sy;
            p.size = size;
            p.progress = elapsed / (float) DURATION;
            p.type = e.type;
            p.color = e.color;
            p.seed = e.seed;
            p.drawHeight = ghostHeight;
            PROJECTIONS.add(p);
        }
    }

    // ============================================================
    // РЕНДЕР
    // ============================================================
    public static void render(DrawContext context, float tickDelta) {
        if (PROJECTIONS.isEmpty()) return;

        for (Proj p : PROJECTIONS) {
            float alpha = 1f - p.progress;
            int a = (int)(alpha * 255);
            if (a <= 3) continue;

            switch (p.type) {
                case RING      -> renderRing(context, p, a);
                case LIGHTNING -> renderLightning(context, p, a);
                case SPIRAL    -> renderSpiral(context, p, a);
                case GHOST     -> renderGhost(context, p, a);
                default -> {}
            }
        }
    }

    // ============================================================
    // GHOST — спрайт-анимация призрака
    // ============================================================
    private static void renderGhost(DrawContext context, Proj p, int a) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        int frame = (int)(p.progress * GHOST_TOTAL);
        if (frame >= GHOST_TOTAL) frame = GHOST_TOTAL - 1;

        int col = frame % GHOST_COLS;
        int row = frame / GHOST_COLS;

        int u = col * GHOST_FRAME_W;
        int v = row * GHOST_FRAME_H;

        // Размер = хитбокс
        int drawH = (int)p.drawHeight;
        int drawW = (int)(drawH * (GHOST_FRAME_W / (float)GHOST_FRAME_H));

        // Спавн из ГОЛОВЫ — низ ghost на cy, растёт вверх
        int px = p.cx - drawW / 2;
        int py = p.cy - drawH;

        float alphaMod = (p.progress > 0.85f) ? (1f - p.progress) / 0.15f : 1f;
        int fa = (int)(a * alphaMod);
        if (fa <= 3) {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            return;
        }

        context.setShaderColor(1f, 1f, 1f, fa / 255f);

        context.getMatrices().push();
        context.getMatrices().translate(px, py, 0);
        context.getMatrices().scale(
            (float)drawW / GHOST_FRAME_W,
            (float)drawH / GHOST_FRAME_H,
            1f);
        context.drawTexture(GHOST_TEXTURE,
            0, 0, u, v,
            GHOST_FRAME_W, GHOST_FRAME_H,
            GHOST_TEX_W, GHOST_TEX_H);
        context.getMatrices().pop();

        context.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // ============================================================
    // RING
    // ============================================================
    private static void renderRing(DrawContext context, Proj p, int a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float t = p.progress;
        float eased = 1f - (1f - t) * (1f - t);
        int r = (int)(p.size * (0.3f + eased * 4.5f));

        int thickness = Math.max(2, (int)(p.size * 0.15f * (1f - t * 0.5f)));

        int segments = 64;
        for (int layer = 4; layer >= 1; layer--) {
            int la = (int)(a * (layer == 1 ? 0.9f : 0.12f * layer));
            if (la <= 2) continue;
            int color = (la << 24) | p.color;
            int offsetR = (layer - 1) * 3;

            for (int i = 0; i < segments; i++) {
                double angle1 = 2 * Math.PI * i / segments;
                double angle2 = 2 * Math.PI * (i + 1) / segments;
                int x1 = p.cx + (int)(Math.cos(angle1) * (r + offsetR));
                int y1 = p.cy + (int)(Math.sin(angle1) * (r + offsetR) * 0.4);
                int x2 = p.cx + (int)(Math.cos(angle2) * (r + offsetR));
                int y2 = p.cy + (int)(Math.sin(angle2) * (r + offsetR) * 0.4);
                drawLine(context, x1, y1, x2, y2, color, layer == 1 ? thickness : 1);
            }
        }
        RenderSystem.disableBlend();
    }

    // ============================================================
    // LIGHTNING
    // ============================================================
    private static void renderLightning(DrawContext context, Proj p, int a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Random r = new Random(p.seed);
        float flicker = 0.7f + RNG.nextFloat() * 0.3f;
        a = (int)(a * flicker);

        int segments = 12;
        int topY = p.cy - (int)(p.size * 10);
        int bottomY = p.cy;

        int[][] points = new int[segments + 1][2];
        points[0][0] = p.cx + (int)((r.nextDouble() - 0.5) * p.size * 0.5);
        points[0][1] = topY;

        for (int i = 1; i <= segments; i++) {
            float t = i / (float)segments;
            int y = (int)MathHelper.lerp(t, topY, bottomY);
            int offset = (int)((r.nextDouble() - 0.5) * p.size * 1.5 * (1f - t * 0.7f));
            points[i][0] = p.cx + offset;
            points[i][1] = y;
        }

        int glowColor1 = ((int)(a * 0.10f) << 24) | p.color;
        int glowColor2 = ((int)(a * 0.20f) << 24) | p.color;
        int glowColor3 = ((int)(a * 0.40f) << 24) | p.color;
        int colorCore = (a << 24) | p.color;
        int white = (a << 24) | 0xFFFFFF;

        for (int i = 0; i < segments; i++) {
            drawLine(context, points[i][0], points[i][1], points[i+1][0], points[i+1][1], glowColor1, 14);
            drawLine(context, points[i][0], points[i][1], points[i+1][0], points[i+1][1], glowColor2, 8);
            drawLine(context, points[i][0], points[i][1], points[i+1][0], points[i+1][1], glowColor3, 4);
            drawLine(context, points[i][0], points[i][1], points[i+1][0], points[i+1][1], colorCore, 2);
            drawLine(context, points[i][0], points[i][1], points[i+1][0], points[i+1][1], white, 1);
        }

        for (int i = 0; i < 8; i++) {
            int idx = r.nextInt(segments);
            int sparkX = points[idx][0] + (int)((r.nextDouble() - 0.5) * 20);
            int sparkY = points[idx][1] + (int)((r.nextDouble() - 0.5) * 10);
            int sparkA = (int)(a * 0.8f);
            context.fill(sparkX - 1, sparkY - 1, sparkX + 1, sparkY + 1, (sparkA << 24) | 0xFFFFFF);
        }
        RenderSystem.disableBlend();
    }

    // ============================================================
    // SPIRAL
    // ============================================================
    private static void renderSpiral(DrawContext context, Proj p, int a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int points = 24;
        float baseAngle = p.progress * 720f;
        float spiralRise = p.progress * p.size * 3f;

        for (int i = 0; i < points; i++) {
            float t = i / (float)points;
            float pointAlpha = (1f - t) * (1f - p.progress * 0.7f);
            int pa = (int)(a * pointAlpha);
            if (pa <= 2) continue;

            float angle = (float)Math.toRadians(baseAngle + i * 20f);
            float radius = p.size * (1.5f + t * 0.6f);
            float rise = (i * 3f) + spiralRise;

            int px = p.cx + (int)(Math.cos(angle) * radius);
            int py = p.cy - (int)rise + (int)(Math.sin(angle) * radius * 0.3);

            float dotSize = p.size * 0.15f * (1f - t * 0.6f);
            int ds = (int)dotSize + 1;

            int glowA = (int)(pa * 0.3f);
            context.fill(px - ds * 2, py - ds * 2, px + ds * 2, py + ds * 2,
                (glowA << 24) | p.color);
            context.fill(px - ds, py - ds, px + ds, py + ds, (pa << 24) | p.color);

            if (i == points - 1 && pa > 80) {
                context.fill(px - ds / 2, py - ds / 2, px + ds / 2, py + ds / 2,
                    (pa << 24) | 0xFFFFFF);
            }
        }
        RenderSystem.disableBlend();
    }

    // ============================================================
    // ХЕЛПЕРЫ
    // ============================================================
    private static void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int thickness) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) return;
        int half = thickness / 2;
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            ctx.fill(x - half, y - half, x - half + thickness, y - half + thickness, color);
        }
    }
}